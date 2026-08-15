package com.aliahmed.Vercel.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Parses GitHub's commit JSON (nested), trimmed to what the history view needs.
 * Flattened into {@link CommitResponse} before it leaves the backend.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubCommitResponse(
        String sha,
        Commit commit,
        @JsonProperty("html_url") String htmlUrl
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Commit(String message, Author author) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Author(String name, String date) {
    }
}
