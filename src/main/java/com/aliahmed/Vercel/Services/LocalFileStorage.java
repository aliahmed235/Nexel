package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Stores built sites on the local filesystem, one folder per deployment, under
 * the configured base path (a mounted volume in production so it survives
 * restarts). The serving layer later reads {@code <base>/<deploymentId>/}.
 */
@Service
@RequiredArgsConstructor
public class LocalFileStorage implements StorageService {

    private final AppProperties properties;

    @Override
    public void store(Long deploymentId, Path outputDir) {
        Path target = Path.of(properties.getStorage().getPath(), String.valueOf(deploymentId));
        try {
            deleteRecursively(target);
            Files.createDirectories(target);
            copyRecursively(outputDir, target);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store deployment " + deploymentId, e);
        }
    }

    @Override
    public Optional<Path> resolve(Long deploymentId, String requestPath) {
        Path base = Path.of(properties.getStorage().getPath(), String.valueOf(deploymentId)).normalize();

        String relative = requestPath == null ? "" : requestPath;
        while (relative.startsWith("/")) {
            relative = relative.substring(1);
        }

        Path target = base.resolve(relative).normalize();
        // Traversal guard: a resolved "../" that escapes the deployment is rejected.
        if (!target.startsWith(base)) {
            return Optional.empty();
        }
        if (Files.isDirectory(target)) {
            target = target.resolve("index.html");
        }
        return Files.isRegularFile(target) ? Optional.of(target) : Optional.empty();
    }

    private void copyRecursively(Path source, Path target) throws IOException {
        try (Stream<Path> walk = Files.walk(source)) {
            for (Path path : (Iterable<Path>) walk::iterator) {
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
}
