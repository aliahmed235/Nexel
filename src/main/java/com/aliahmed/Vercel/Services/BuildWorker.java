package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.config.AppProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * The consumer: a single background thread that blocks on the queue, pulls one
 * deployment id at a time, and hands it to {@link BuildService}. Enabled by a
 * flag so a pure-API instance can run without a worker — the same code moves to
 * a dedicated machine later just by turning the flag on there and off here.
 */
@Component
@RequiredArgsConstructor
public class BuildWorker {

    private static final Logger log = LoggerFactory.getLogger(BuildWorker.class);
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(5);

    private final BuildQueue buildQueue;
    private final BuildService buildService;
    private final AppProperties properties;

    private volatile boolean running = true;
    private Thread worker;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!properties.getWorker().isEnabled()) {
            log.info("Build worker is disabled (app.worker.enabled=false) — not consuming the queue");
            return;
        }
        worker = new Thread(this::loop, "build-worker");
        worker.setDaemon(true);
        worker.start();
        log.info("Build worker started, blocking on the build queue");
    }

    private void loop() {
        while (running) {
            try {
                buildQueue.dequeue(POLL_TIMEOUT).ifPresent(this::safeProcess);
            } catch (Exception e) {
                // Never let a queue error kill the loop; pause briefly and retry.
                log.error("Build worker loop error", e);
                sleepQuietly();
            }
        }
        log.info("Build worker stopped");
    }

    private void safeProcess(Long deploymentId) {
        try {
            buildService.process(deploymentId);
        } catch (Exception e) {
            // BuildService already records FAILED; this guards the loop itself.
            log.error("Unexpected error processing deployment {}", deploymentId, e);
        }
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
    }
}
