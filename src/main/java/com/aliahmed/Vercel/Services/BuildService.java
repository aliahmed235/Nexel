package com.aliahmed.Vercel.Services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Runs one build end to end: fetch the source, build it, store the output, and
 * move the deployment's status. Deliberately not transactional — the slow steps
 * happen here, and only {@link DeploymentStatusService} touches the database, in
 * short transactions around them.
 */
@Service
@RequiredArgsConstructor
public class BuildService {

    private static final Logger log = LoggerFactory.getLogger(BuildService.class);

    private final DeploymentStatusService statusService;
    private final SourceFetchService sourceFetchService;
    private final List<SiteBuilder> siteBuilders;
    private final StorageService storageService;
    private final FrameworkDetector frameworkDetector;

    public void process(Long deploymentId) {
        Optional<BuildContext> maybeContext = statusService.markBuilding(deploymentId);
        if (maybeContext.isEmpty()) {
            log.warn("Deployment {} no longer exists — skipping phantom queue entry", deploymentId);
            return;
        }

        BuildContext context = maybeContext.get();
        log.info("Building deployment {} ({} @ {})",
                deploymentId, context.repoFullName(), context.ref());

        StringBuilder buildLog = new StringBuilder();
        SourceFetchService.FetchedSource source = null;
        try {
            source = sourceFetchService.fetch(context.userId(), context.repoFullName(), context.ref());
            Path buildRoot = resolveBuildRoot(source.root(), context.rootDirectory());
            detectFrameworkAndRoot(deploymentId, source.root(), context.rootDirectory());
            String basePath = "/sites/" + context.subdomain() + "/";
            Path output = selectBuilder(buildRoot).build(buildRoot, basePath, buildLog);
            storageService.store(deploymentId, output);
            statusService.markReadyAndCurrent(deploymentId);
            log.info("Deployment {} is READY", deploymentId);
        } catch (Exception e) {
            log.error("Deployment {} FAILED: {}", deploymentId, e.getMessage(), e);
            buildLog.append("\n\nBuild failed: ").append(e.getMessage()).append("\n");
            statusService.markFailed(deploymentId, e.getMessage());
        } finally {
            storeBuildLog(deploymentId, buildLog);
            if (source != null) {
                cleanup(source.workDir());
            }
        }
    }

    /** Persist the captured build output. Best-effort — a log failure must not fail the build. */
    private void storeBuildLog(Long deploymentId, StringBuilder buildLog) {
        try {
            storageService.storeLog(deploymentId, buildLog.toString());
        } catch (RuntimeException e) {
            log.warn("Could not store build log for deployment {}: {}", deploymentId, e.getMessage());
        }
    }

    /**
     * Detects the framework and the app folder, then records them on the project.
     * Best-effort: it runs alongside a working build and must never fail it, so any
     * problem is logged and swallowed.
     */
    private void detectFrameworkAndRoot(Long deploymentId, Path repoRoot, String rootDirectory) {
        try {
            FrameworkDetector.Result result = frameworkDetector.inspect(repoRoot, rootDirectory);
            statusService.recordDetection(deploymentId, result.framework(), result.rootDirectory());
            log.info("Detected framework={} rootDirectory={} for deployment {}",
                    result.framework(), result.rootDirectory(), deploymentId);
        } catch (RuntimeException e) {
            log.warn("Framework detection failed for deployment {}: {}", deploymentId, e.getMessage());
        }
    }

    /**
     * The directory to build in. With no root directory set, that's the repo root
     * (and a builder may still auto-detect a subfolder). When set, it's that
     * subfolder — validated to exist and to stay inside the repo, so a crafted
     * value can't point the build at an arbitrary path on the worker.
     */
    private Path resolveBuildRoot(Path repoRoot, String rootDirectory) {
        if (rootDirectory == null || rootDirectory.isBlank()) {
            return repoRoot;
        }
        Path candidate = repoRoot.resolve(rootDirectory).normalize();
        if (!candidate.startsWith(repoRoot)) {
            throw new IllegalStateException(
                    "Root directory '" + rootDirectory + "' escapes the repository");
        }
        if (!Files.isDirectory(candidate)) {
            throw new IllegalStateException(
                    "Root directory '" + rootDirectory + "' does not exist in the repository");
        }
        log.info("Building in root directory: {}", rootDirectory);
        return candidate;
    }

    /** First builder (by @Order) that can handle the repo. Static is the fallback. */
    private SiteBuilder selectBuilder(Path source) {
        return siteBuilders.stream()
                .filter(builder -> builder.supports(source))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No site builder can handle this repository"));
    }

    private void cleanup(Path directory) {
        try (Stream<Path> walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // best-effort cleanup of a temp dir
                }
            });
        } catch (IOException e) {
            log.warn("Failed to clean up build directory {}", directory, e);
        }
    }
}
