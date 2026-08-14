package com.aliahmed.Vercel.Services;

/**
 * A detached snapshot of everything the worker needs to run one build, captured
 * inside a transaction so the worker can do its slow work (download, build,
 * copy) without holding a database transaction or a lazy Hibernate session open.
 */
public record BuildContext(
        Long deploymentId,
        Long userId,
        String repoFullName,
        String ref,
        String rootDirectory,
        String subdomain
) {
}
