package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.dto.GithubRepoResponse;

import java.util.List;

/**
 * Reads a user's repositories from GitHub. Kept as an interface so tests can
 * substitute a fake instead of reaching the network. Mirrors
 * {@link GithubOAuthClient}, which does the same for the OAuth endpoints.
 */
public interface GithubRepoClient {
    List<GithubRepoResponse> listRepos(Long userId);

    GithubRepoResponse getRepo(Long userId, String fullName);
}
