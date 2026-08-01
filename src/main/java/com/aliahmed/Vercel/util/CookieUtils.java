package com.aliahmed.Vercel.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

public final class CookieUtils {

    private CookieUtils() {
    }

    /**
     * SameSite=Lax is required, not incidental: the OAuth callback is a
     * top-level navigation coming from github.com, and Lax is the strictest
     * setting that still lets the cookie ride along with it.
     */
    public static ResponseCookie build(String name, String value, String path,
                                       Duration maxAge, boolean secure) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge)
                .build();
    }

    public static ResponseCookie expire(String name, String path, boolean secure) {
        return build(name, "", path, Duration.ZERO, secure);
    }

    public static Optional<String> read(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }
}
