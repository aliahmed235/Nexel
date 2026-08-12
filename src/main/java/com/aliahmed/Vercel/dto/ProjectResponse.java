package com.aliahmed.Vercel.dto;

import java.time.Instant;

public record ProjectResponse(
        Long id,
        Long githubRepoId,
        String repoFullName,
        String defaultBranch,
        String subdomain,
        String framework,
        String rootDirectory,
        String url,
        Instant createdAt
) {
}
