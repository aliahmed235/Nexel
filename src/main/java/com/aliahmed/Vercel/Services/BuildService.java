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

    public void process(Long deploymentId) {
        Optional<BuildContext> maybeContext = statusService.markBuilding(deploymentId);
        if (maybeContext.isEmpty()) {
            log.warn("Deployment {} no longer exists — skipping phantom queue entry", deploymentId);
            return;
        }

        BuildContext context = maybeContext.get();
        log.info("Building deployment {} ({} @ {})",
                deploymentId, context.repoFullName(), context.ref());

        SourceFetchService.FetchedSource source = null;
        try {
            source = sourceFetchService.fetch(context.userId(), context.repoFullName(), context.ref());
            Path output = selectBuilder(source.root()).build(source.root());
            storageService.store(deploymentId, output);
            statusService.markReadyAndCurrent(deploymentId);
            log.info("Deployment {} is READY", deploymentId);
        } catch (Exception e) {
            log.error("Deployment {} FAILED: {}", deploymentId, e.getMessage(), e);
            statusService.markFailed(deploymentId, e.getMessage());
        } finally {
            if (source != null) {
                cleanup(source.workDir());
            }
        }
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
