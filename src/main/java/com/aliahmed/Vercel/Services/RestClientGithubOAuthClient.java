package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.config.GithubOAuthProperties;
import com.aliahmed.Vercel.dto.GithubEmailResponse;
import com.aliahmed.Vercel.dto.GithubTokenResponse;
import com.aliahmed.Vercel.dto.GithubUserResponse;
import com.aliahmed.Vercel.exception.GithubOAuthException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Service
public class RestClientGithubOAuthClient implements GithubOAuthClient {

    private static final String API_VERSION_HEADER = "X-GitHub-Api-Version";
    private static final String API_VERSION = "2022-11-28";

    private final GithubOAuthProperties properties;
    private final RestClient restClient;

    public RestClientGithubOAuthClient(GithubOAuthProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.build();
    }

    @Override
    public GithubTokenResponse exchangeCodeForToken(String code) {
        GithubTokenResponse response = restClient.post()
                .uri(properties.getTokenUrl())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "client_id", properties.getClientId(),
                        "client_secret", properties.getClientSecret(),
                        "code", code))
                .retrieve()
                .body(GithubTokenResponse.class);

        if (response == null || !response.isSuccess()) {
            String reason = response == null ? "empty response" : response.errorDescription();
            throw new GithubOAuthException("GitHub rejected the authorization code: " + reason);
        }
        return response;
    }

    @Override
    public GithubUserResponse fetchAuthenticatedUser(String accessToken) {
        GithubUserResponse user = restClient.get()
                .uri(properties.getApiBaseUrl() + "/user")
                .header("Authorization", "Bearer " + accessToken)
                .header(API_VERSION_HEADER, API_VERSION)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(GithubUserResponse.class);

        if (user == null || user.id() == null) {
            throw new GithubOAuthException("GitHub returned no user for the issued token");
        }
        return user;
    }

    @Override
    public Optional<String> fetchPrimaryEmail(String accessToken) {
        GithubEmailResponse[] emails = restClient.get()
                .uri(properties.getApiBaseUrl() + "/user/emails")
                .header("Authorization", "Bearer " + accessToken)
                .header(API_VERSION_HEADER, API_VERSION)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(GithubEmailResponse[].class);

        if (emails == null) {
            return Optional.empty();
        }
        return Arrays.stream(emails)
                .filter(GithubEmailResponse::primary)
                .filter(GithubEmailResponse::verified)
                .map(GithubEmailResponse::email)
                .findFirst();
    }
}
