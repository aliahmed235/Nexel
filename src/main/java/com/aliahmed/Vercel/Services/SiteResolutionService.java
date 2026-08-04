package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.Repositories.DeploymentRepository;
import com.aliahmed.Vercel.Repositories.ProjectRepository;
import com.aliahmed.Vercel.entity.Deployment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Turns a site's subdomain into the deployment whose files should be served:
 * the project's current (live) deployment.
 *
 * <p>This lookup runs on every page request, so it's the natural place to add a
 * Redis cache later (phase 6) — the mapping subdomain → deployment id rarely
 * changes and is safe to cache with invalidation on a new successful build.
 */
@Service
@RequiredArgsConstructor
public class SiteResolutionService {

    private final ProjectRepository projectRepository;
    private final DeploymentRepository deploymentRepository;

    @Transactional(readOnly = true)
    public Optional<Long> currentDeploymentId(String subdomain) {
        return projectRepository.findBySubdomain(subdomain)
                .flatMap(project -> deploymentRepository.findByProjectIdAndCurrentTrue(project.getId()))
                .map(Deployment::getId);
    }
}
