package com.aliahmed.Vercel.Services;

import java.time.Duration;
import java.util.Optional;

/**
 * The hand-off point between the API and the build worker. The API pushes a
 * deployment id; the worker blocks on the other end and pulls it.
 *
 * <p>An interface so tests can enqueue into a fake, and so the backing store
 * could change without touching the deployment logic.
 */
public interface BuildQueue {

    /** Pushes a deployment id onto the queue for a worker to pick up. */
    void enqueue(Long deploymentId);

    /**
     * Blocking pop (BRPOP). Waits up to {@code timeout} for a job, then returns
     * empty if none arrived — the empty return lets the worker loop check for
     * shutdown between waits rather than blocking forever.
     */
    Optional<Long> dequeue(Duration timeout);

    /** How many jobs are waiting. Handy for monitoring and tests. */
    long size();
}
