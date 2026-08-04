package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.config.GithubOAuthProperties;
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
}
