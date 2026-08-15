package com.aliahmed.Vercel.Services;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * The "reading" build: the repository already contains the finished site, so
 * there is nothing to compile. If the repo committed its output into a common
 * folder (dist/build/public/out with an index.html), that folder is served;
 * otherwise the repo root is.
 *
 * <p>Lowest priority so that, once a NodeDockerBuilder exists, it gets first
 * refusal and this remains the fallback for repos that need no build.
 */
@Service
@Order(Integer.MAX_VALUE)
public class StaticSiteBuilder implements SiteBuilder {

    private static final List<String> COMMON_OUTPUT_DIRS = List.of("dist", "build", "public", "out");

    @Override
    public boolean supports(Path source) {
        return true;
    }

    @Override
    public Path build(Path source, String basePath, StringBuilder buildLog) {
        // basePath is irrelevant here — a pre-built/static site is served exactly as committed.
        buildLog.append("No build step detected — serving the repository's files as-is.\n");
        for (String dir : COMMON_OUTPUT_DIRS) {
            Path candidate = source.resolve(dir);
            if (Files.isDirectory(candidate) && Files.exists(candidate.resolve("index.html"))) {
                return candidate;
            }
        }
        return source;
    }
}
