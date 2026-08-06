package com.aliahmed.Vercel.dto;

import java.time.Instant;

public record DeploymentResponse(
        Long id,
        Long projectId,
        String status,
        String commitSha,
        boolean current,
        String errorMessage,
        String url,
        Instant createdAt,
        Instant readyAt
) {
}
