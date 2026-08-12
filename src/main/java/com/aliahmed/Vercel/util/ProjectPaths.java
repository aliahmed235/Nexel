package com.aliahmed.Vercel.util;

/**
 * Normalises the user-supplied "root directory" (the subfolder to build in) into
 * a safe, repo-relative path — or null when blank. Rejects absolute paths and any
 * {@code ".."} segment so a build can never be pointed outside the fetched repo.
 */
public final class ProjectPaths {

    private ProjectPaths() {
    }

    public static String normalizeRootDirectory(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replace('\\', '/');
        while (value.startsWith("./")) {
            value = value.substring(2);
        }
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.isEmpty()) {
            return null;
        }
        for (String segment : value.split("/")) {
            if (segment.equals("..")) {
                throw new IllegalArgumentException("rootDirectory must not contain '..'");
            }
        }
        return value;
    }
}
