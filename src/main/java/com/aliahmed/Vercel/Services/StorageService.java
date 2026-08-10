package com.aliahmed.Vercel.Services;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Where a built site's files come to rest. An interface so the backend — local
 * disk or R2 object storage — can change without touching the build pipeline or
 * the serving layer.
 */
public interface StorageService {

    /** Copies the built output so it becomes the served content for this deployment. */
    void store(Long deploymentId, Path outputDir);

    /**
     * Resolves a request path within a deployment to a servable file, or empty
     * if it doesn't exist. A directory (or the root) resolves to its
     * {@code index.html}. Guards against escaping the deployment's own files.
     */
    Optional<StoredObject> resolve(Long deploymentId, String requestPath);

    /** Removes a deployment's stored files. Best-effort; safe if already gone. */
    void delete(Long deploymentId);
}
