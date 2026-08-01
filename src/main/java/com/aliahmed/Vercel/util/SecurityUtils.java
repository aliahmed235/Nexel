package com.aliahmed.Vercel.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class SecurityUtils {

    private static final SecureRandom RANDOM = new SecureRandom();

    private SecurityUtils() {
    }

    /** URL-safe random string, used for the OAuth state parameter. */
    public static String generateSecureToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Compares without short-circuiting on the first differing byte, so the
     * time taken reveals nothing about how much of the value was correct.
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
