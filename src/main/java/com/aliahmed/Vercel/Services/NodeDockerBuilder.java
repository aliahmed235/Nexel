package com.aliahmed.Vercel.Services;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Builds a Node project by running {@code npm install && npm run build} inside a
 * throwaway {@code node:20} container, then handing back the generated static
 * output. Runs on the VM, where a Docker daemon exists.
 *
 * <p>Tried before {@link StaticSiteBuilder} (lower {@code @Order}); it only
 * claims repos that actually have a build script, so plain HTML sites still fall
 * through to the static builder.
 */
@Service
@Order(100)
@RequiredArgsConstructor
public class NodeDockerBuilder implements SiteBuilder {

    private static final Logger log = LoggerFactory.getLogger(NodeDockerBuilder.class);

    private static final String NODE_IMAGE = "node:20";
    private static final Duration BUILD_TIMEOUT = Duration.ofMinutes(5);
    private static final long MEMORY_LIMIT_GB = 2;
    /** Where common frameworks drop their static output. Vite=dist, CRA=build, Next export=out. */
    private static final List<String> OUTPUT_DIRS = List.of("dist", "build", "out");

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(Path source) {
        Path packageJson = source.resolve("package.json");
        if (!Files.isRegularFile(packageJson)) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(packageJson.toFile());
            return root.path("scripts").path("build").isTextual();
        } catch (RuntimeException e) {
            log.warn("Could not read package.json at {}", packageJson, e);
            return false;
        }
    }

    @Override
    public Path build(Path source) {
        runContainerBuild(source);
        return locateOutput(source);
    }

    private void runContainerBuild(Path source) {
        // Run as the host user so build output is owned by the worker, not root,
        // and keep npm's cache/home inside the container's writable /tmp.
        String command = String.join(" ",
                "timeout", Long.toString(BUILD_TIMEOUT.toSeconds()),
                "docker", "run", "--rm",
                "--user", "\"$(id -u):$(id -g)\"",
                "-e", "HOME=/tmp",
                "-e", "npm_config_cache=/tmp/.npm",
                "-v", "\"" + source.toAbsolutePath() + "\":/app",
                "-w", "/app",
                "--memory", MEMORY_LIMIT_GB + "g",
                NODE_IMAGE,
                "sh", "-c", "\"npm install && npm run build\"");

        log.info("Running Node build: {}", command);
        String output = runShell(command);
        log.info("Build output:\n{}", output);
    }

    private String runShell(String command) {
        Process process;
        try {
            process = new ProcessBuilder("bash", "-lc", command)
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
            throw new IllegalStateException("Could not start the build process", e);
        }

        String output;
        try {
            output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(BUILD_TIMEOUT.toSeconds() + 60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Build timed out after " + BUILD_TIMEOUT.toMinutes() + " minutes");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed reading build output", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IllegalStateException("Build was interrupted", e);
        }

        if (process.exitValue() != 0) {
            throw new IllegalStateException(
                    "npm build failed (exit " + process.exitValue() + "):\n" + tail(output));
        }
        return output;
    }

    private Path locateOutput(Path source) {
        for (String dir : OUTPUT_DIRS) {
            Path candidate = source.resolve(dir);
            if (Files.isDirectory(candidate)) {
                log.info("Using build output directory: {}", dir);
                return candidate;
            }
        }
        throw new IllegalStateException(
                "No static output found (looked for dist/build/out). This project may need a "
                        + "server to run (SSR), which static hosting can't serve.");
    }

    /** Keep the last chunk of build logs for the error message. */
    private String tail(String output) {
        int max = 2000;
        return output.length() <= max ? output : output.substring(output.length() - max);
    }
}
