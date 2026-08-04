package com.aliahmed.Vercel.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * A Redis list used as a FIFO queue: the API {@code LPUSH}es deployment ids on
 * one end, and the worker {@code BRPOP}s them off the other. The blocking pop
 * is what lets a worker sleep with zero CPU until a job arrives.
 */
@Service
@RequiredArgsConstructor
public class RedisBuildQueue implements BuildQueue {

    /** The shared list key. The worker on the VPS reads from the same key. */
    static final String QUEUE_KEY = "build_queue";

    private final StringRedisTemplate redis;

    @Override
    public void enqueue(Long deploymentId) {
        redis.opsForList().leftPush(QUEUE_KEY, String.valueOf(deploymentId));
    }

    @Override
    public long size() {
        Long length = redis.opsForList().size(QUEUE_KEY);
        return length == null ? 0 : length;
    }
}
