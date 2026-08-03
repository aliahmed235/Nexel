package com.aliahmed.Vercel.entity;

/**
 * The lifecycle of a single build.
 *
 * <pre>
 *   QUEUED ──▶ BUILDING ──┬──▶ READY   (becomes the live deployment)
 *                         └──▶ FAILED
 * </pre>
 *
 * Phase 3.1 only ever creates QUEUED; the worker moves it forward.
 */
public enum DeploymentStatus {
    QUEUED,
    BUILDING,
    READY,
    FAILED
}
