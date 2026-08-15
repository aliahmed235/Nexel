package com.aliahmed.Vercel.dto;

/**
 * Optional body for triggering a deployment. {@code commit} pins the build to a
 * specific commit SHA (deploy history); null/absent builds the project's branch tip.
 */
public record TriggerDeploymentRequest(String commit) {
}
