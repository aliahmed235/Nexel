package com.aliahmed.Vercel.dto;

public record CurrentUserResponse(
        Long id,
        String githubLogin,
        String name,
        String email,
        String avatarUrl
) {
}
