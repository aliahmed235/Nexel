package com.aliahmed.Vercel.config;

import jakarta.annotation.PostConstruct;
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
    private final AuthCode authCode = new AuthCode();

    public String githubCallbackUrl() {
        return baseUrl + "/api/auth/github/callback";
    }

    /**
     * Catches the two misconfigurations that otherwise surface as an opaque
     * GitHub error page rather than a startup failure.
     */
    @PostConstruct
    void validate() {
        requireAbsoluteUrl(baseUrl, "app.base-url", "APP_BASE_URL");
        requireAbsoluteUrl(frontendUrl, "app.frontend-url", "APP_FRONTEND_URL");
    }

    private void requireAbsoluteUrl(String value, String propertyName, String envVarName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    propertyName + " (env " + envVarName + ") is not set.");
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            throw new IllegalStateException(
                    propertyName + " (env " + envVarName + ") must start with http:// or https://, got: " + value);
        }
        if (value.endsWith("/")) {
            throw new IllegalStateException(
                    propertyName + " (env " + envVarName + ") must not end with a slash, got: " + value);
        }
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
    public static class AuthCode {
        private Duration ttl = Duration.ofMinutes(5);
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
