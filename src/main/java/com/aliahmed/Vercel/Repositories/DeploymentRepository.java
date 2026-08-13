package com.aliahmed.Vercel.Repositories;

import com.aliahmed.Vercel.entity.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeploymentRepository extends JpaRepository<Deployment, Long> {

    List<Deployment> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    /**
     * Just the ids, as a scalar projection — no {@code Deployment} entities enter the
     * persistence context. That lets a project delete rely on the DB's ON DELETE
     * CASCADE without Hibernate flushing managed children that point at the removed project.
     */
    @Query("select d.id from Deployment d where d.project.id = :projectId")
    List<Long> findIdsByProjectId(@Param("projectId") Long projectId);

    /** Scopes a deployment to its project; the project is separately verified to be the caller's. */
    Optional<Deployment> findByIdAndProjectId(Long id, Long projectId);

    /** The project's live deployment, if any. Used to unset it when a new build wins. */
    Optional<Deployment> findByProjectIdAndCurrentTrue(Long projectId);
}
