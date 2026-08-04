package com.aliahmed.Vercel.Services;

/**
 * The hand-off point between the API and the build worker. The API pushes a
 * deployment id; a worker (phase 3.3, on a separate machine) blocks on the
 * other end and pulls it.
 *
 * <p>An interface so tests can enqueue into a fake, and so the backing store
 * could change without touching the deployment logic.
 */
public interface BuildQueue {

    /** Pushes a deployment id onto the queue for a worker to pick up. */
    void enqueue(Long deploymentId);

    /** How many jobs are waiting. Handy for monitoring and tests. */
    long size();
}
