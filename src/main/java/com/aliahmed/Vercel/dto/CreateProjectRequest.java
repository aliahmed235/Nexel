package com.aliahmed.Vercel.dto;

import com.aliahmed.Vercel.util.ProjectPaths;

/**
 * Request to connect a repository. Only the full name is required — everything
 * else (repo id, default branch) is read from GitHub as the source of truth.
 * {@code branch} is an optional override of the repo's default branch.
 * {@code rootDirectory} is the subfolder to build in for a monorepo (e.g.
 * "Client"); null/blank means auto-detect. {@code defaultPath} is an optional
 * landing path appended to the site URL; null/blank keeps it at "/".
 */
public record CreateProjectRequest(String repoFullName, String branch, String rootDirectory, String defaultPath) {

    public CreateProjectRequest {
        if (repoFullName == null || repoFullName.isBlank()) {
            throw new IllegalArgumentException("repoFullName is required");
        }
        repoFullName = repoFullName.trim();
        if (!repoFullName.contains("/")) {
            throw new IllegalArgumentException("repoFullName must be in the form owner/repo");
        }
        branch = (branch == null || branch.isBlank()) ? null : branch.trim();
        rootDirectory = ProjectPaths.normalizeRootDirectory(rootDirectory);
        defaultPath = ProjectPaths.normalizeDefaultPath(defaultPath);
    }
}
