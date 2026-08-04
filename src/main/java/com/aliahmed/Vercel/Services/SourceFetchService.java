package com.aliahmed.Vercel.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads a repository's source from GitHub and unpacks it into a temporary
 * working directory.
 */
@Service
@RequiredArgsConstructor
public class SourceFetchService {

    private final GithubRepoClient githubRepoClient;

    /**
     * @param root    the repository's top-level directory, to build/serve from
     * @param workDir the temp directory to delete when the build is done
     */
    public record FetchedSource(Path root, Path workDir) {
    }

    public FetchedSource fetch(Long userId, String repoFullName, String ref) throws IOException {
        byte[] zip = githubRepoClient.downloadRepoZip(userId, repoFullName, ref);
        Path workDir = Files.createTempDirectory("vercel-build-");
        unzip(zip, workDir);
        return new FetchedSource(resolveRoot(workDir), workDir);
    }

    private void unzip(byte[] zip, Path destination) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path target = destination.resolve(entry.getName()).normalize();
                // Zip-slip guard: never let an entry write outside the target dir.
                if (!target.startsWith(destination)) {
                    throw new IOException("Zip entry escapes the target directory: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * GitHub archives wrap everything in a single top-level folder named
     * {@code owner-repo-sha}. Unwrap it so the build sees the repo root directly.
     */
    private Path resolveRoot(Path workDir) throws IOException {
        try (Stream<Path> entries = Files.list(workDir)) {
            List<Path> top = entries.toList();
            if (top.size() == 1 && Files.isDirectory(top.get(0))) {
                return top.get(0);
            }
            return workDir;
        }
    }
}
