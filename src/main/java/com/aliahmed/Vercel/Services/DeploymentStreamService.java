package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.dto.DeploymentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the open SSE connections, keyed by deployment id, and fans an event out to
 * everyone watching that deployment. In-memory and per-instance: because every instance
 * subscribes to the Redis channel, each forwards to its own connections, so this scales
 * horizontally with no shared state.
 */
@Service
public class DeploymentStreamService {

    private static final long TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /** Opens a stream for one deployment and immediately sends its current status. */
    public SseEmitter subscribe(Long deploymentId, DeploymentEvent current) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.computeIfAbsent(deploymentId, k -> ConcurrentHashMap.newKeySet()).add(emitter);
        emitter.onCompletion(() -> remove(deploymentId, emitter));
        emitter.onTimeout(() -> remove(deploymentId, emitter));
        emitter.onError(e -> remove(deploymentId, emitter));
        sendTo(emitter, deploymentId, current);
        return emitter;
    }

    /** Pushes a status change to every browser watching that deployment. */
    public void dispatch(DeploymentEvent event) {
        Set<SseEmitter> set = emitters.get(event.deploymentId());
        if (set == null) {
            return;
        }
        for (SseEmitter emitter : set) {
            sendTo(emitter, event.deploymentId(), event);
        }
    }

    private void sendTo(SseEmitter emitter, Long deploymentId, DeploymentEvent event) {
        try {
            emitter.send(SseEmitter.event().name("status").data(event));
        } catch (IOException | RuntimeException e) {
            remove(deploymentId, emitter);
        }
    }

    private void remove(Long deploymentId, SseEmitter emitter) {
        Set<SseEmitter> set = emitters.get(deploymentId);
        if (set != null) {
            set.remove(emitter);
            if (set.isEmpty()) {
                emitters.remove(deploymentId);
            }
        }
    }
}
