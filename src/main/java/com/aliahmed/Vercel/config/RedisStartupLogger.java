package com.aliahmed.Vercel.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Prints, once at startup, which Redis host the application actually resolved.
 * A quick way to tell "the env var never arrived" (logs localhost) from "the
 * var is set but the network is unreachable" (logs the real host).
 */
@Component
@RequiredArgsConstructor
public class RedisStartupLogger {

    private static final Logger log = LoggerFactory.getLogger(RedisStartupLogger.class);

    private final RedisConnectionFactory connectionFactory;

    @EventListener(ApplicationReadyEvent.class)
    public void logRedisTarget() {
        String envUrl = System.getenv("REDIS_URL");
        log.info("REDIS_URL env var present: {}", envUrl != null && !envUrl.isBlank());

        if (connectionFactory instanceof LettuceConnectionFactory lettuce) {
            log.info("Redis is configured to connect to {}:{}", lettuce.getHostName(), lettuce.getPort());
        } else {
            log.info("Redis connection factory: {}", connectionFactory.getClass().getSimpleName());
        }
    }
}
