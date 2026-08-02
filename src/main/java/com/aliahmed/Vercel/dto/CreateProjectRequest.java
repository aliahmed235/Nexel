package com.aliahmed.Vercel.dto;

/**
 * Request to connect a repository. Only the full name is required — everything
 * else (repo id, default branch) is read from GitHub as the source of truth.
 * {@code branch} is an optional override of the repo's default branch.
 */
public record CreateProjectRequest(String repoFullName, String branch) {

    public CreateProjectRequest {
        if (repoFullName == null || repoFullName.isBlank()) {
            throw new IllegalArgumentException("repoFullName is required");
        }
        repoFullName = repoFullName.trim();
        if (!repoFullName.contains("/")) {
            throw new IllegalArgumentException("repoFullName must be in the form owner/repo");
        }
        branch = (branch == null || branch.isBlank()) ? null : branch.trim();
    }
}
