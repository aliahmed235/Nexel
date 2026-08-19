package com.aliahmed.Vercel.Repositories;

import com.aliahmed.Vercel.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** The user's projects that have at least one deployment (i.e. Deploy was triggered). Newest first. */
    @Query("select p from Project p where p.user.id = :userId "
            + "and exists (select 1 from Deployment d where d.project = p) "
            + "order by p.createdAt desc")
    List<Project> findDeployedByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /** Ownership-scoped lookup — a user can only reach their own projects. */
    Optional<Project> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndGithubRepoId(Long userId, Long githubRepoId);

    /** The user's existing connection for a repo, if any — used to make connect idempotent. */
    Optional<Project> findByUserIdAndGithubRepoId(Long userId, Long githubRepoId);

    boolean existsBySubdomain(String subdomain);

    /** Resolves an incoming site request's subdomain to its project. */
    Optional<Project> findBySubdomain(String subdomain);

    /** Matches an incoming GitHub push (by its webhook id header) back to its project. */
    Optional<Project> findByGithubHookId(Long githubHookId);
}
