package com.aliahmed.Vercel.dto;

import java.time.Instant;

/**
 * The single error shape every JSON endpoint returns, so the frontend has one
 * thing to parse rather than one per failure mode.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path);
    }
}
