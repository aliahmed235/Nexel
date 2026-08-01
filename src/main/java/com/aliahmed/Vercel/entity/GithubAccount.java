package com.aliahmed.Vercel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
 * The GitHub credential for a user. Kept apart from {@link User} so the access
 * token has its own lifecycle: it can be rotated or revoked without touching
 * identity, and it is never loaded unless something needs to call GitHub.
 */
@Entity
@Table(name = "github_accounts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GithubAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** AES-GCM ciphertext. Never log or expose this. */
    @Column(name = "access_token_enc", nullable = false, columnDefinition = "text")
    private String accessTokenEnc;

    @Column(name = "token_type")
    private String tokenType;

    private String scope;

    @Column(name = "connected_at", nullable = false, updatable = false)
    private Instant connectedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        connectedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
