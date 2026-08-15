package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.config.R2Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Stores built sites in Cloudflare R2 (S3-compatible). Active when
 * {@code app.storage.type=r2}.
 *
 * <p>The VM worker calls {@link #store} to upload; the Railway server calls
 * {@link #resolve} to read and serve. Objects are keyed
 * {@code deployments/<id>/<path>}, so both sides address the same file the same way.
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "r2")
@RequiredArgsConstructor
public class R2Storage implements StorageService {

    private final S3Client s3;
    private final R2Properties properties;

    @Override
    public void store(Long deploymentId, Path outputDir) {
        String prefix = keyPrefix(deploymentId);
        deleteByPrefix(prefix);
        try (Stream<Path> walk = Files.walk(outputDir)) {
            walk.filter(Files::isRegularFile).forEach(file -> upload(prefix, outputDir, file));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read build output for deployment " + deploymentId, e);
        }
    }

    @Override
    public Optional<StoredObject> resolve(Long deploymentId, String requestPath) {
        String prefix = keyPrefix(deploymentId);

        String relative = requestPath == null ? "" : requestPath;
        while (relative.startsWith("/")) {
            relative = relative.substring(1);
        }

        // Emulate directory-index behaviour: R2 has no folders, so map "/" and
        // extension-less paths to their index.html.
        List<String> candidates = new ArrayList<>();
        if (relative.isEmpty()) {
            candidates.add("index.html");
        } else if (relative.endsWith("/")) {
            candidates.add(relative + "index.html");
        } else {
            candidates.add(relative);
            if (!relative.contains(".")) {
                candidates.add(relative + "/index.html");
            }
        }

        for (String candidate : candidates) {
            Optional<StoredObject> found = tryGet(prefix + candidate, candidate);
            if (found.isPresent()) {
                return found;
            }
        }
        // SPA fallback: an extension-less path is a client-side route (e.g. /products),
        // so serve the app's index.html and let the browser's router handle it. A missing
        // asset (something with a file extension) still 404s.
        if (!looksLikeAsset(relative)) {
            Optional<StoredObject> shell = tryGet(prefix + "index.html", "index.html");
            if (shell.isPresent()) {
                return shell;
            }
        }
        return Optional.empty();
    }

    /** A path whose last segment contains a "." is a file request (asset), not a route. */
    private boolean looksLikeAsset(String path) {
        int slash = path.lastIndexOf('/');
        String last = slash >= 0 ? path.substring(slash + 1) : path;
        return last.contains(".");
    }

    @Override
    public void delete(Long deploymentId) {
        deleteByPrefix(keyPrefix(deploymentId));
        s3.deleteObject(builder -> builder.bucket(properties.getBucket()).key(logKey(deploymentId)));
    }

    @Override
    public void storeLog(Long deploymentId, String log) {
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(properties.getBucket())
                        .key(logKey(deploymentId))
                        .contentType(MediaType.TEXT_PLAIN_VALUE)
                        .build(),
                RequestBody.fromString(log == null ? "" : log));
    }

    @Override
    public Optional<String> readLog(Long deploymentId) {
        try {
            return Optional.of(s3.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(logKey(deploymentId))
                    .build()).asUtf8String());
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }

    /** Logs live outside the deployments/ prefix so they aren't wiped by store() or served. */
    private String logKey(Long deploymentId) {
        return "logs/" + deploymentId + ".log";
    }

    private void upload(String prefix, Path root, Path file) {
        String key = prefix + root.relativize(file).toString().replace('\\', '/');
        String contentType = MediaTypeFactory.getMediaType(file.getFileName().toString())
                .map(MediaType::toString)
                .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        s3.putObject(
                PutObjectRequest.builder().bucket(properties.getBucket()).key(key).contentType(contentType).build(),
                RequestBody.fromFile(file));
    }

    private Optional<StoredObject> tryGet(String key, String filenameForType) {
        try {
            ResponseBytes<GetObjectResponse> object = s3.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(properties.getBucket()).key(key).build());
            MediaType contentType = MediaTypeFactory.getMediaType(filenameForType)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);
            return Optional.of(new StoredObject(new ByteArrayResource(object.asByteArray()), contentType));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }

    /** Removes every object under a deployment's prefix. Fine for the small sites we host. */
    private void deleteByPrefix(String prefix) {
        ListObjectsV2Response listing = s3.listObjectsV2(
                ListObjectsV2Request.builder().bucket(properties.getBucket()).prefix(prefix).build());
        if (listing.contents().isEmpty()) {
            return;
        }
        List<ObjectIdentifier> ids = listing.contents().stream()
                .map(o -> ObjectIdentifier.builder().key(o.key()).build())
                .toList();
        s3.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(properties.getBucket())
                .delete(Delete.builder().objects(ids).build())
                .build());
    }

    private String keyPrefix(Long deploymentId) {
        return "deployments/" + deploymentId + "/";
    }
}
