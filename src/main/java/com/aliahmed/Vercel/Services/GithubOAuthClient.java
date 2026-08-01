package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.dto.GithubTokenResponse;
import com.aliahmed.Vercel.dto.GithubUserResponse;

import java.util.Optional;

/**
 * The only seam through which this application talks to GitHub's OAuth and
 * user endpoints. Kept as an interface so tests can substitute a fake instead
 * of reaching the network.
 */
public interface GithubOAuthClient {

    /** Trades the single-use authorization code for an access token. */
    GithubTokenResponse exchangeCodeForToken(String code);

    /** Reads the profile of whoever owns the token. */
    GithubUserResponse fetchAuthenticatedUser(String accessToken);

    /**
     * Falls back to the verified primary address when the user hides their
     * email on their public profile.
     */
    Optional<String> fetchPrimaryEmail(String accessToken);
}
