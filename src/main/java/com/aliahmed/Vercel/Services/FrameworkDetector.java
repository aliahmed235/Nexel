package com.aliahmed.Vercel.Services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Works out the two facts Vercel shows on its import screen: which folder the app
 * lives in, and which framework it uses. Both come from one scan — find the folder
 * that holds {@code package.json}, then read that file's dependencies. The framework
 * is a <em>result</em> of finding the folder, never the other way round.
 *
 * <p>Read-only and defensive: any failure yields "unknown" rather than throwing, so
 * detection can never fail a build.
 */
@Service
@RequiredArgsConstructor
public class FrameworkDetector {

    private static final Logger log = LoggerFactory.getLogger(FrameworkDetector.class);

    private final ObjectMapper objectMapper;

    /**
     * @param framework     e.g. "vite", "nextjs", "create-react-app", "static", or null
     * @param rootDirectory the folder the app was found in, relative to the repo root
     *                      ("" / root becomes null)
     */
    public record Result(String framework, String rootDirectory) {
    }

    /**
     * @param repoRoot      the fetched repository root
     * @param rootDirectory the user's chosen subfolder, or null/blank to auto-detect
     */
    public Result inspect(Path repoRoot, String rootDirectory) {
        Path folder = (rootDirectory != null && !rootDirectory.isBlank())
                ? repoRoot.resolve(rootDirectory)
                : findAppFolder(repoRoot);

        if (!Files.isDirectory(folder)) {
            return new Result(null, null);
        }
        return new Result(detectFramework(folder), toRelative(repoRoot, folder));
    }

    /**
     * Repo root if it holds a buildable app, else the first subfolder that does. Uses the
     * same "package.json with a build script" rule the builder uses to pick the folder, so
     * the detected/auto-filled root directory always matches what actually gets built —
     * a plain package.json with no build script (docs tooling, config) is skipped.
     */
    private Path findAppFolder(Path repoRoot) {
        if (hasBuildScript(repoRoot)) {
            return repoRoot;
        }
        try (Stream<Path> children = Files.list(repoRoot)) {
            return children
                    .filter(Files::isDirectory)
                    .sorted()
                    .filter(this::hasBuildScript)
                    .findFirst()
                    .orElse(repoRoot);
        } catch (IOException e) {
            log.warn("Could not scan {} while detecting the framework", repoRoot, e);
            return repoRoot;
        }
    }

    private boolean hasBuildScript(Path dir) {
        JsonNode pkg = readPackageJson(dir);
        return pkg != null && pkg.path("scripts").path("build").isTextual();
    }

    private String detectFramework(Path folder) {
        JsonNode pkg = readPackageJson(folder);
        if (pkg == null) {
            return hasIndexHtml(folder) ? "static" : null;
        }
        // Tooling/framework-specific first; generic react/vue last, since a Vite+React
        // app has BOTH "vite" and "react" and we want the preset ("vite"), like Vercel.
        if (dependsOn(pkg, "next")) return "nextjs";
        if (dependsOn(pkg, "nuxt")) return "nuxt";
        if (dependsOn(pkg, "@sveltejs/kit")) return "sveltekit";
        if (dependsOn(pkg, "@angular/core")) return "angular";
        if (dependsOn(pkg, "gatsby")) return "gatsby";
        if (dependsOn(pkg, "vite")) return "vite";
        if (dependsOn(pkg, "react-scripts")) return "create-react-app";
        if (dependsOn(pkg, "vue")) return "vue";
        if (dependsOn(pkg, "react")) return "react";
        return hasIndexHtml(folder) ? "static" : null;
    }

    private boolean dependsOn(JsonNode pkg, String dependency) {
        return pkg.path("dependencies").has(dependency) || pkg.path("devDependencies").has(dependency);
    }

    private JsonNode readPackageJson(Path folder) {
        Path packageJson = folder.resolve("package.json");
        if (!Files.isRegularFile(packageJson)) {
            return null;
        }
        try {
            return objectMapper.readTree(packageJson.toFile());
        } catch (RuntimeException e) {
            log.warn("Could not read package.json at {}", packageJson, e);
            return null;
        }
    }

    private boolean hasIndexHtml(Path dir) {
        return Files.isRegularFile(dir.resolve("index.html"));
    }

    private String toRelative(Path repoRoot, Path folder) {
        String relative = repoRoot.relativize(folder).toString().replace('\\', '/');
        return relative.isBlank() ? null : relative;
    }
}
