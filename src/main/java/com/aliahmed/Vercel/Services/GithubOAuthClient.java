package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.dto.GithubTokenResponse;
import com.aliahmed.Vercel.dto.GithubUserResponse;

import java.util.Optional;
public interface GithubOAuthClient {

    GithubTokenResponse exchangeCodeForToken(String code);

    GithubUserResponse fetchAuthenticatedUser(String accessToken);

    Optional<String> fetchPrimaryEmail(String accessToken);
}
