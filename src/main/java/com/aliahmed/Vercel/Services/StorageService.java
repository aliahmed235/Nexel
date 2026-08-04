package com.aliahmed.Vercel.Services;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Where a built site's files come to rest. An interface so the local-disk
 * implementation can later be swapped for object storage (S3/R2) without
 * touching the build pipeline.
 */
public interface StorageService {

    /** Copies the built output so it becomes the served content for this deployment. */
    void store(Long deploymentId, Path outputDir);

    /**
     * Resolves a request path within a deployment to a real file to serve, or
     * empty if it doesn't exist. A directory (or the root) resolves to its
     * {@code index.html}. Guards against path traversal outside the deployment.
     */
    Optional<Path> resolve(Long deploymentId, String requestPath);
}
