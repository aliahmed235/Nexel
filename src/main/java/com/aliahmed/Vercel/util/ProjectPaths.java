package com.aliahmed.Vercel.util;

/**
 * Normalises user-supplied relative paths into a safe form — or null when blank.
 * Used for the "root directory" (the subfolder to build in) and the "default path"
 * (the landing path appended to a site's URL). Rejects absolute paths and any
 * {@code ".."} segment so neither a build nor a URL can be pointed outside its scope.
 */
public final class ProjectPaths {

    private ProjectPaths() {
    }

    public static String normalizeRootDirectory(String raw) {
        return normalizeRelative(raw, "rootDirectory");
    }

    public static String normalizeDefaultPath(String raw) {
        return normalizeRelative(raw, "defaultPath");
    }

    private static String normalizeRelative(String raw, String field) {
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
                throw new IllegalArgumentException(field + " must not contain '..'");
            }
        }
        return value;
    }
}
