package com.aliahmed.Vercel.Services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * A Redis list used as a FIFO queue: the API {@code LPUSH}es deployment ids on
 * one end, and the worker {@code BRPOP}s them off the other. The blocking pop
 * is what lets a worker sleep with zero CPU until a job arrives.
 */
@Service
@RequiredArgsConstructor
public class RedisBuildQueue implements BuildQueue {

    private static final Logger log = LoggerFactory.getLogger(RedisBuildQueue.class);

    /** The shared list key. A worker on any machine reads from the same key. */
    static final String QUEUE_KEY = "build_queue";

    private final StringRedisTemplate redis;

    @Override
    public void enqueue(Long deploymentId) {
        redis.opsForList().leftPush(QUEUE_KEY, String.valueOf(deploymentId));
    }

    @Override
    public Optional<Long> dequeue(Duration timeout) {
        String value = redis.opsForList().rightPop(QUEUE_KEY, timeout);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(value.trim()));
        } catch (NumberFormatException e) {
            // A malformed entry can't correspond to a deployment; drop it rather
            // than crash the worker loop.
            log.warn("Discarding non-numeric build queue entry: {}", value);
            return Optional.empty();
        }
    }

    @Override
    public long size() {
        Long length = redis.opsForList().size(QUEUE_KEY);
        return length == null ? 0 : length;
    }
}
