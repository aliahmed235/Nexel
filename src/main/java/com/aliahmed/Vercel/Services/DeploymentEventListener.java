package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.dto.DeploymentEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Receives deployment status events from Redis — published by any instance, including the
 * remote worker — and hands them to the local SSE connections.
 */
@Component
@RequiredArgsConstructor
public class DeploymentEventListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(DeploymentEventListener.class);

    private final DeploymentStreamService streamService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            DeploymentEvent event = objectMapper.readValue(message.getBody(), DeploymentEvent.class);
            streamService.dispatch(event);
        } catch (RuntimeException e) {
            log.warn("Could not handle a deployment event: {}", e.getMessage());
        }
    }
}
