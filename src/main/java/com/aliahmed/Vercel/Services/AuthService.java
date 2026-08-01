package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.config.AppProperties;
import com.aliahmed.Vercel.config.GithubOAuthProperties;
import com.aliahmed.Vercel.dto.GithubTokenResponse;
import com.aliahmed.Vercel.dto.GithubUserResponse;
import com.aliahmed.Vercel.entity.User;
import com.aliahmed.Vercel.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;

/**
 * The login flow, start to finish. The controller above it does nothing but
 * translate this into cookies and redirects.
 */
@Service
public class AuthService {

    private static final int STATE_BYTES = 32;

    private final GithubOAuthClient githubClient;
    private final UserService userService;
    private final GithubAccountService githubAccountService;
    private final JwtService jwtService;
    private final AppProperties appProperties;
    private final GithubOAuthProperties githubProperties;

    public AuthService(GithubOAuthClient githubClient,
                       UserService userService,
                       GithubAccountService githubAccountService,
                       JwtService jwtService,
                       AppProperties appProperties,
                       GithubOAuthProperties githubProperties) {
        this.githubClient = githubClient;
        this.userService = userService;
        this.githubAccountService = githubAccountService;
        this.jwtService = jwtService;
        this.appProperties = appProperties;
        this.githubProperties = githubProperties;
    }

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
     * Exchanges the authorization code, upserts the user, stores the encrypted
     * GitHub token, and returns this application's own JWT.
     */
    @Transactional
    public String completeLogin(String code) {
        GithubTokenResponse token = githubClient.exchangeCodeForToken(code);
        GithubUserResponse profile = githubClient.fetchAuthenticatedUser(token.accessToken());

        String email = profile.email() != null
                ? profile.email()
                : githubClient.fetchPrimaryEmail(token.accessToken()).orElse(null);

        User user = userService.findOrCreateFromGithub(profile, email);
        githubAccountService.storeToken(user, token);

        return jwtService.issue(user);
    }

    /**
     * Where the browser is sent once a token has been minted. The token goes in
     * the fragment, not the query string, because fragments are never sent to a
     * server and so cannot land in an Nginx access log.
     */
    public String buildFrontendRedirect(String jwt) {
        return appProperties.getFrontendUrl()
                + appProperties.getFrontendCallbackPath()
                + "#token=" + jwt
                + "&expires_in=" + jwtService.expirySeconds();
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
