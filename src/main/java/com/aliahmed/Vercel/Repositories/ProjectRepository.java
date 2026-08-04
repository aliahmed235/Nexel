package com.aliahmed.Vercel.Repositories;

import com.aliahmed.Vercel.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Ownership-scoped lookup — a user can only reach their own projects. */
    Optional<Project> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndGithubRepoId(Long userId, Long githubRepoId);

    boolean existsBySubdomain(String subdomain);

    /** Resolves an incoming site request's subdomain to its project. */
    Optional<Project> findBySubdomain(String subdomain);
}
