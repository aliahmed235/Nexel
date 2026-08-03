package com.aliahmed.Vercel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One build of a {@link Project}. Immutable per build — a new push produces a
 * new row rather than mutating an old one, which is what makes deployment
 * history and rollback possible.
 */
@Entity
@Table(name = "deployments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Deployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeploymentStatus status;

    /** The commit this build was made from. Filled by the worker at clone time. */
    @Column(name = "commit_sha", length = 64)
    private String commitSha;

    /** True for the single live deployment of the project. */
    @Column(name = "is_current", nullable = false)
    private boolean current;

    /** Populated only when {@link DeploymentStatus#FAILED}. */
    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Set when the build reaches {@link DeploymentStatus#READY}. */
    @Column(name = "ready_at")
    private Instant readyAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = DeploymentStatus.QUEUED;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
