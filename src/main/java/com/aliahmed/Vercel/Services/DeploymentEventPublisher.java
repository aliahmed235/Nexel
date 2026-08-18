package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.dto.DeploymentEvent;
import com.aliahmed.Vercel.entity.DeploymentStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

/**
 * Announces deployment status changes on a Redis pub/sub channel so the API — which may
 * run on a different machine than the worker that changed the status — can push them to
 * browsers over SSE. Published <em>after</em> the transaction commits, so no client is
 * ever told about a status that then rolls back. Best-effort: a missed event never fails
 * the build or request that produced it.
 */
@Service
@RequiredArgsConstructor
public class DeploymentEventPublisher {

    /** The channel every instance publishes to and the API subscribes to. */
    public static final String CHANNEL = "deployment-events";

    private static final Logger log = LoggerFactory.getLogger(DeploymentEventPublisher.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public void publish(Long deploymentId, Long projectId, DeploymentStatus status) {
        DeploymentEvent event = new DeploymentEvent(deploymentId, projectId, status.name());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(event);
                }
            });
        } else {
            send(event);
        }
    }

    private void send(DeploymentEvent event) {
        try {
            redis.convertAndSend(CHANNEL, objectMapper.writeValueAsString(event));
        } catch (RuntimeException e) {
            log.warn("Could not publish deployment event for {}: {}", event.deploymentId(), e.getMessage());
        }
    }
}
