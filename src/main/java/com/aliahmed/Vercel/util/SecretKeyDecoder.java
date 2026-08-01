package com.aliahmed.Vercel.util;

import java.util.Base64;

/**
 * Decodes a base64 secret from configuration.
 *
 * <p>Secrets are pasted by hand into dashboards, so they arrive with stray
 * quotes, trailing newlines and copied whitespace. Those are stripped here.
 * Anything still malformed fails at startup naming the exact property and
 * environment variable, because the raw decoder only reports a byte offset.
 */
public final class SecretKeyDecoder {

    private SecretKeyDecoder() {
    }

    public static byte[] decode(String value, String propertyName, String envVarName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    propertyName + " (env " + envVarName + ") is not set. "
                            + "Generate one with: openssl rand -base64 32");
        }

        String cleaned = value.trim();
        if (cleaned.length() >= 2
                && (cleaned.startsWith("\"") && cleaned.endsWith("\"")
                || cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }

        try {
            return Base64.getDecoder().decode(cleaned);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    propertyName + " (env " + envVarName + ") is not valid base64. "
                            + "Check for quotes, spaces or a line break around the value. "
                            + "Generate a clean one with: openssl rand -base64 32", e);
        }
    }
}
