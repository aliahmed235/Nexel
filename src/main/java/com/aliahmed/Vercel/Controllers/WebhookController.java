package com.aliahmed.Vercel.Controllers;

import com.aliahmed.Vercel.Services.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives GitHub push events. Public — trust comes from the HMAC signature the
 * {@link WebhookService} verifies, not a JWT. The body is taken as raw bytes because
 * the signature is computed over the exact payload GitHub sent.
 */
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/github")
    public ResponseEntity<Void> github(
            @RequestBody(required = false) byte[] payload,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestHeader(value = "X-GitHub-Hook-ID", required = false) String hookId) {
        webhookService.handlePush(payload == null ? new byte[0] : payload, signature, event, hookId);
        return ResponseEntity.ok().build();
    }
}
