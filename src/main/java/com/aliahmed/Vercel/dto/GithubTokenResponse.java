package com.aliahmed.Vercel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GitHub returns 200 OK even when the exchange fails, with {@code error} set
 * instead of {@code access_token}. Both shapes are captured here.
 */
public record GithubTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        String scope,
        String error,
        @JsonProperty("error_description") String errorDescription
) {
    public boolean isSuccess() {
        return accessToken != null && !accessToken.isBlank();
    }
}
