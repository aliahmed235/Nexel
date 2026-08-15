package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.Repositories.ProjectRepository;
import com.aliahmed.Vercel.config.AppProperties;
import com.aliahmed.Vercel.entity.Project;
import com.aliahmed.Vercel.exception.WebhookException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Turns a verified GitHub push into a deployment.
 *
 * <p>The endpoint is public, so trust comes entirely from the HMAC signature: GitHub
 * signs each payload with our shared secret, and we recompute it over the raw bytes and
 * compare in constant time. A push is matched back to its project by the webhook id
 * header, then deployed only if it landed on the project's deployed branch.
 */
@Service
@RequiredArgsConstructor
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String BRANCH_REF_PREFIX = "refs/heads/";

    private final AppProperties properties;
    private final ProjectRepository projectRepository;
    private final DeploymentService deploymentService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handlePush(byte[] rawBody, String signatureHeader, String event, String hookIdHeader) {
        verifySignature(rawBody, signatureHeader);

        if (!"push".equalsIgnoreCase(event)) {
            return; // ping and other events are acknowledged but ignored
        }

        Long hookId = parseLong(hookIdHeader);
        if (hookId == null) {
            return;
        }
        Project project = projectRepository.findByGithubHookId(hookId).orElse(null);
        if (project == null) {
            return; // a push for a project we no longer track
        }

        JsonNode payload = objectMapper.readTree(rawBody);
        String branch = branchFromRef(payload.path("ref").asString(""));
        if (!project.getDefaultBranch().equals(branch)) {
            return; // only the deployed branch triggers a build
        }

        String commit = payload.path("after").asString(null);
        deploymentService.deployOnPush(project, commit);
        log.info("Auto-deploying {} from a push to {} ({})", project.getRepoFullName(), branch, commit);
    }

    private void verifySignature(byte[] body, String signatureHeader) {
        String secret = properties.getWebhook().getSecret();
        if (secret == null || secret.isBlank()) {
            throw new WebhookException("Webhooks are not configured");
        }
        if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            throw new WebhookException("Missing or malformed signature");
        }
        String expected = SIGNATURE_PREFIX + hmacHex(secret, body);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signatureHeader.getBytes(StandardCharsets.UTF_8))) {
            throw new WebhookException("Signature does not match");
        }
    }

    private String hmacHex(String secret, byte[] body) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(body);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new WebhookException("Could not compute the signature", e);
        }
    }

    private String branchFromRef(String ref) {
        return ref.startsWith(BRANCH_REF_PREFIX) ? ref.substring(BRANCH_REF_PREFIX.length()) : ref;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
