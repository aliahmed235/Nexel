package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
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
 * Stores built sites on the local filesystem, one folder per deployment. The
 * default backend; active unless {@code app.storage.type=r2}.
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalFileStorage implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorage.class);

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
    public Optional<StoredObject> resolve(Long deploymentId, String requestPath) {
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
        if (!Files.isRegularFile(target)) {
            return Optional.empty();
        }
        MediaType contentType = MediaTypeFactory.getMediaType(target.getFileName().toString())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return Optional.of(new StoredObject(new FileSystemResource(target), contentType));
    }

    @Override
    public void delete(Long deploymentId) {
        Path target = Path.of(properties.getStorage().getPath(), String.valueOf(deploymentId));
        try {
            deleteRecursively(target);
        } catch (IOException e) {
            // Cleanup is best-effort — a leftover folder shouldn't fail a disconnect.
            log.warn("Failed to delete stored files for deployment {}", deploymentId, e);
        }
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
