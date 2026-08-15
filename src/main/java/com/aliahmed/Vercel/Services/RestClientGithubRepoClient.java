package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.config.GithubOAuthProperties;
import com.aliahmed.Vercel.dto.CommitResponse;
import com.aliahmed.Vercel.dto.GithubCommitResponse;
import com.aliahmed.Vercel.dto.GithubHookResponse;
import com.aliahmed.Vercel.dto.GithubRepoResponse;
import com.aliahmed.Vercel.exception.GithubOAuthException;
import com.aliahmed.Vercel.exception.ResourceNotFoundException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class RestClientGithubRepoClient implements GithubRepoClient {

    private static final String API_VERSION_HEADER = "X-GitHub-Api-Version";
    private static final String API_VERSION = "2022-11-28";

    private final GithubOAuthProperties properties;
    private final GithubAccountService githubAccountService;
    private final RestClient restClient;

    public RestClientGithubRepoClient(GithubOAuthProperties properties,
                                      GithubAccountService githubAccountService,
                                      RestClient.Builder builder) {
        this.properties = properties;
        this.githubAccountService = githubAccountService;
        this.restClient = builder.build();
    }

    @Override
    public List<GithubRepoResponse> listRepos(Long userId) {
        String token = githubAccountService.accessTokenFor(userId);
        try {
            GithubRepoResponse[] repos = restClient.get()
                    .uri(properties.getApiBaseUrl() + "/user/repos?per_page=100&sort=updated&visibility=all")
                    .header("Authorization", "Bearer " + token)
                    .header(API_VERSION_HEADER, API_VERSION)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(GithubRepoResponse[].class);

            return repos == null ? List.of() : Arrays.asList(repos);
        } catch (RestClientException e) {
            throw new GithubOAuthException("Failed to list repositories from GitHub", e);
        }
    }

    @Override
    public GithubRepoResponse getRepo(Long userId, String fullName) {
        String token = githubAccountService.accessTokenFor(userId);
        try {
            GithubRepoResponse repo = restClient.get()
                    .uri(properties.getApiBaseUrl() + "/repos/" + fullName)
                    .header("Authorization", "Bearer " + token)
                    .header(API_VERSION_HEADER, API_VERSION)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(GithubRepoResponse.class);

            if (repo == null || repo.githubRepoId() == null) {
                throw new ResourceNotFoundException("Repository not found or not accessible: " + fullName);
            }
            return repo;
        } catch (HttpClientErrorException.NotFound | HttpClientErrorException.Forbidden e) {
            // 404 or 403 both mean "this user can't connect this repo" — don't
            // leak which, and don't treat it as a server error.
            throw new ResourceNotFoundException("Repository not found or not accessible: " + fullName);
        } catch (RestClientException e) {
            throw new GithubOAuthException("Failed to fetch repository " + fullName + " from GitHub", e);
        }
    }

    @Override
    public byte[] downloadRepoZip(Long userId, String fullName, String ref) {
        String token = githubAccountService.accessTokenFor(userId);
        try {
            // GitHub answers with a 302 to codeload; the RestClient follows it.
            byte[] zip = restClient.get()
                    .uri(properties.getApiBaseUrl() + "/repos/" + fullName + "/zipball/" + ref)
                    .header("Authorization", "Bearer " + token)
                    .header(API_VERSION_HEADER, API_VERSION)
                    .retrieve()
                    .body(byte[].class);

            if (zip == null || zip.length == 0) {
                throw new GithubOAuthException("GitHub returned an empty archive for " + fullName);
            }
            return zip;
        } catch (HttpClientErrorException.NotFound | HttpClientErrorException.Forbidden e) {
            throw new ResourceNotFoundException("Repository archive not found or not accessible: " + fullName);
        } catch (RestClientException e) {
            throw new GithubOAuthException("Failed to download archive for " + fullName, e);
        }
    }

    @Override
    public List<CommitResponse> listCommits(Long userId, String fullName, String branch, int limit) {
        String token = githubAccountService.accessTokenFor(userId);
        try {
            GithubCommitResponse[] commits = restClient.get()
                    .uri(properties.getApiBaseUrl() + "/repos/" + fullName + "/commits?sha=" + branch + "&per_page=" + limit)
                    .header("Authorization", "Bearer " + token)
                    .header(API_VERSION_HEADER, API_VERSION)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(GithubCommitResponse[].class);

            if (commits == null) {
                return List.of();
            }
            return Arrays.stream(commits).map(this::toCommitResponse).toList();
        } catch (HttpClientErrorException.NotFound | HttpClientErrorException.Forbidden e) {
            throw new ResourceNotFoundException("Repository not found or not accessible: " + fullName);
        } catch (RestClientException e) {
            throw new GithubOAuthException("Failed to list commits for " + fullName, e);
        }
    }

    @Override
    public Long createPushWebhook(Long userId, String fullName, String callbackUrl, String secret) {
        String token = githubAccountService.accessTokenFor(userId);
        Map<String, Object> body = Map.of(
                "name", "web",
                "active", true,
                "events", List.of("push"),
                "config", Map.of("url", callbackUrl, "content_type", "json", "secret", secret));
        try {
            GithubHookResponse hook = restClient.post()
                    .uri(properties.getApiBaseUrl() + "/repos/" + fullName + "/hooks")
                    .header("Authorization", "Bearer " + token)
                    .header(API_VERSION_HEADER, API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(GithubHookResponse.class);
            return hook == null ? null : hook.id();
        } catch (RestClientException e) {
            throw new GithubOAuthException("Failed to register a webhook for " + fullName, e);
        }
    }

    @Override
    public void deleteWebhook(Long userId, String fullName, Long hookId) {
        if (hookId == null) {
            return;
        }
        String token = githubAccountService.accessTokenFor(userId);
        try {
            restClient.delete()
                    .uri(properties.getApiBaseUrl() + "/repos/" + fullName + "/hooks/" + hookId)
                    .header("Authorization", "Bearer " + token)
                    .header(API_VERSION_HEADER, API_VERSION)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            // Best-effort: a leftover hook on GitHub does no harm.
        }
    }

    private CommitResponse toCommitResponse(GithubCommitResponse c) {
        GithubCommitResponse.Commit commit = c.commit();
        String message = commit == null ? null : commit.message();
        GithubCommitResponse.Author author = commit == null ? null : commit.author();
        return new CommitResponse(
                c.sha(),
                message,
                author == null ? null : author.name(),
                author == null ? null : author.date(),
                c.htmlUrl());
    }
}
