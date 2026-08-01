package com.aliahmed.Vercel.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github.oauth")
@Getter
@Setter
public class GithubOAuthProperties {

    private String clientId;

    private String clientSecret;

    /**
     * Trimmed on read. These are pasted into a dashboard by hand, and a
     * trailing newline turns into %0A in the authorize URL, which GitHub
     * answers with an unexplained 404.
     */
    public String getClientId() {
        return clientId == null ? null : clientId.trim();
    }

    public String getClientSecret() {
        return clientSecret == null ? null : clientSecret.trim();
    }

    /**
     * read:user  — profile
     * user:email — primary address when the public one is hidden
     * repo       — clone private repositories and register webhooks
     */
    private String scope = "read:user user:email repo";

    private String authorizeUrl = "https://github.com/login/oauth/authorize";

    private String tokenUrl = "https://github.com/login/oauth/access_token";

    private String apiBaseUrl = "https://api.github.com";
}
