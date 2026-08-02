package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.config.AppProperties;
import com.aliahmed.Vercel.config.GithubOAuthProperties;
import com.aliahmed.Vercel.dto.AuthTokenResponse;
import com.aliahmed.Vercel.dto.GithubTokenResponse;
import com.aliahmed.Vercel.dto.GithubUserResponse;
import com.aliahmed.Vercel.entity.User;
import com.aliahmed.Vercel.mapper.UserMapper;
import com.aliahmed.Vercel.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;

/**
 * The login flow, start to finish. The controller above it does nothing but
 * translate this into cookies and redirects.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int STATE_BYTES = 32;

    private final GithubOAuthClient githubClient;
    private final UserService userService;
    private final GithubAccountService githubAccountService;
    private final AuthCodeService authCodeService;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final AppProperties appProperties;
    private final GithubOAuthProperties githubProperties;

    /** An unguessable value echoed back by GitHub, proving the callback belongs to this browser. */
    public String generateState() {
        return SecurityUtils.generateSecureToken(STATE_BYTES);
    }

    public String buildAuthorizeUrl(String state) {
        return UriComponentsBuilder.fromUriString(githubProperties.getAuthorizeUrl())
                .queryParam("client_id", githubProperties.getClientId())
                .queryParam("redirect_uri", appProperties.githubCallbackUrl())
                .queryParam("scope", githubProperties.getScope())
                .queryParam("state", state)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    /**
     * Exchanges GitHub's authorization code, upserts the user, stores the
     * encrypted GitHub token, and returns a one-time code for the redirect.
     *
     * <p>The JWT is deliberately not returned here. Putting it in the redirect
     * URL would leave it in browser history and in anything that captures the
     * address bar; the frontend trades this short-lived code for it instead.
     */
    @Transactional
    public String completeGithubLogin(String code) {
        GithubTokenResponse token = githubClient.exchangeCodeForToken(code);
        GithubUserResponse profile = githubClient.fetchAuthenticatedUser(token.accessToken());

        String email = profile.email() != null
                ? profile.email()
                : githubClient.fetchPrimaryEmail(token.accessToken()).orElse(null);

        User user = userService.findOrCreateFromGithub(profile, email);
        githubAccountService.storeToken(user, token);

        return authCodeService.issue(user);
    }

    /** Burns a one-time code and issues the actual session token. */
    @Transactional
    public AuthTokenResponse exchangeCode(String oneTimeCode) {
        User user = authCodeService.redeem(oneTimeCode);
        return new AuthTokenResponse(
                jwtService.issue(user),
                jwtService.expirySeconds(),
                userMapper.toCurrentUserResponse(user));
    }

    /** Where the browser is sent once login succeeds. Carries only the one-time code. */
    public String buildFrontendRedirect(String oneTimeCode) {
        return UriComponentsBuilder
                .fromUriString(appProperties.getFrontendUrl() + appProperties.getFrontendCallbackPath())
                .queryParam("code", oneTimeCode)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    public String buildFrontendErrorRedirect(String reason) {
        return UriComponentsBuilder
                .fromUriString(appProperties.getFrontendUrl() + appProperties.getFrontendCallbackPath())
                .queryParam("error", reason)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }
}
