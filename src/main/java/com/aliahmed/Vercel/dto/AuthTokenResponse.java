package com.aliahmed.Vercel.dto;

/**
 * The result of trading a one-time code for a session. The profile is included
 * so the frontend can render the signed-in state without a second round trip.
 */
public record AuthTokenResponse(
        String token,
        long expiresIn,
        CurrentUserResponse user
) {
}
