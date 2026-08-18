package com.aliahmed.Vercel.dto;

/** A deployment status change, carried over Redis pub/sub and pushed to browsers via SSE. */
public record DeploymentEvent(Long deploymentId, Long projectId, String status) {
}
