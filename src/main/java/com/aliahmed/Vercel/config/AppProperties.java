package com.aliahmed.Vercel.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Everything that differs between a laptop and a server lives here, so no URL
 * or secret is ever hardcoded in a class.
 */
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    /** Public origin of this backend, e.g. https://api.example.com. No trailing slash. */
    private String baseUrl;

    /** Where the browser lands after login, e.g. https://app.example.com. */
    private String frontendUrl;

    /** Path on the frontend that reads the token out of the URL fragment. */
    private String frontendCallbackPath = "/auth/callback";

    /** Origins allowed to call this API from a browser. */
    private List<String> corsAllowedOrigins = List.of();

    private final Jwt jwt = new Jwt();
    private final Crypto crypto = new Crypto();
    private final OAuthState oauthState = new OAuthState();

    public String githubCallbackUrl() {
        return baseUrl + "/api/auth/github/callback";
    }

    @Getter
    @Setter
    public static class Jwt {
        /** Base64-encoded signing key, at least 32 bytes decoded. */
        private String secret;
        private Duration expiry = Duration.ofDays(7);
        private String issuer = "vercel-clone";
    }

    @Getter
    @Setter
    public static class Crypto {
        /** Base64-encoded AES key, exactly 32 bytes decoded. */
        private String secret;
    }

    @Getter
    @Setter
    public static class OAuthState {
        private String cookieName = "oauth_state";
        private String cookiePath = "/api/auth";
        private Duration ttl = Duration.ofMinutes(10);
        /** Must be true in production; false only so http://localhost works. */
        private boolean cookieSecure = true;
    }
}
