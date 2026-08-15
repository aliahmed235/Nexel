package com.aliahmed.Vercel.dto;

/**
 * A repository commit, flattened for the deploy-history view. {@code sha} can be
 * passed back as the {@code commit} when triggering a deployment to build that exact point.
 */
public record CommitResponse(
        String sha,
        String message,
        String author,
        String date,
        String url
) {
}
