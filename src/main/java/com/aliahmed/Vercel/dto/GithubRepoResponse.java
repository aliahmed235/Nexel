package com.aliahmed.Vercel.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A GitHub repository, trimmed to what this app needs. Serves double duty: it
 * parses GitHub's (much larger) JSON and is also the shape returned to the
 * frontend, so the repo picker gets exactly these fields.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubRepoResponse(
        @JsonProperty("id") Long githubRepoId,
        String name,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("private") boolean isPrivate,
        @JsonProperty("default_branch") String defaultBranch
) {
}
