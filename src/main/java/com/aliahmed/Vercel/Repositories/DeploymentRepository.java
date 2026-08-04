package com.aliahmed.Vercel.Repositories;

import com.aliahmed.Vercel.entity.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeploymentRepository extends JpaRepository<Deployment, Long> {

    List<Deployment> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    /** Scopes a deployment to its project; the project is separately verified to be the caller's. */
    Optional<Deployment> findByIdAndProjectId(Long id, Long projectId);

    /** The project's live deployment, if any. Used to unset it when a new build wins. */
    Optional<Deployment> findByProjectIdAndCurrentTrue(Long projectId);
}
