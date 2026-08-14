package com.aliahmed.Vercel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * A connected GitHub repository. Phase 2 records the connection; building and
 * serving it come later, so {@code framework} stays null until phase 3 detects
 * it and there are no deployments yet.
 */
@Entity
@Table(name = "projects")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** GitHub's numeric repo id. Stable across renames, unlike the full name. */
    @Column(name = "github_repo_id", nullable = false)
    private Long githubRepoId;

    /** e.g. "aliahmed235/portfolio". */
    @Column(name = "repo_full_name", nullable = false)
    private String repoFullName;

    /** The branch that will be deployed, e.g. "main". */
    @Column(name = "default_branch", nullable = false)
    private String defaultBranch;

    /** Where the site will be served, e.g. "portfolio-a1b2". Globally unique. */
    @Column(nullable = false, unique = true)
    private String subdomain;

    /** Detected in phase 3. Null until then. */
    private String framework;

    /**
     * The subfolder to build in, for monorepos (e.g. "Client"). Null means
     * auto-detect. Like Vercel's "Root Directory" setting. Always repo-relative
     * and validated — never allowed to escape the fetched repo.
     */
    @Column(name = "root_directory")
    private String rootDirectory;

    /**
     * Optional landing path appended to the site URL — e.g. "products" makes the URL
     * {@code .../sites/<subdomain>/products}. Null/blank keeps the URL at the root "/".
     */
    @Column(name = "default_path")
    private String defaultPath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
