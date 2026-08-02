package com.aliahmed.Vercel.util;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Turns a repository name into a DNS-safe subdomain label. The service pairs
 * {@link #slugify} with {@link #randomSuffix} and a uniqueness check, so two
 * users connecting a "portfolio" repo never collide.
 */
public final class Subdomains {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String SUFFIX_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int MAX_BASE_LENGTH = 40;

    private Subdomains() {
    }

    /** Lowercase, hyphen-separated, alphanumeric only. Falls back to "app" if nothing survives. */
    public static String slugify(String input) {
        if (input == null) {
            return "app";
        }
        String slug = input.toLowerCase(Locale.ROOT).trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");

        if (slug.length() > MAX_BASE_LENGTH) {
            slug = slug.substring(0, MAX_BASE_LENGTH).replaceAll("-+$", "");
        }
        return slug.isEmpty() ? "app" : slug;
    }

    public static String randomSuffix(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(SUFFIX_ALPHABET.charAt(RANDOM.nextInt(SUFFIX_ALPHABET.length())));
        }
        return builder.toString();
    }
}
