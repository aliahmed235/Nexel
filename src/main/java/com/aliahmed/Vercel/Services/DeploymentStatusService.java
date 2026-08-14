package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.Repositories.DeploymentRepository;
import com.aliahmed.Vercel.entity.Deployment;
import com.aliahmed.Vercel.entity.DeploymentStatus;
import com.aliahmed.Vercel.entity.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * The short, transactional state changes a build makes to a deployment. Kept
 * apart from {@link BuildService} so the slow work (download, build, copy) runs
 * outside any transaction — only these quick status flips touch the database.
 */
@Service
@RequiredArgsConstructor
public class DeploymentStatusService {

    private static final int MAX_ERROR_LENGTH = 1000;

    private final DeploymentRepository deploymentRepository;

    /**
     * Marks the deployment BUILDING and returns a detached snapshot of what the
     * build needs. Empty if the id no longer exists (a phantom queue entry).
     */
    @Transactional
    public Optional<BuildContext> markBuilding(Long deploymentId) {
        return deploymentRepository.findById(deploymentId).map(deployment -> {
            deployment.setStatus(DeploymentStatus.BUILDING);
            Project project = deployment.getProject();
            return new BuildContext(
                    deployment.getId(),
                    project.getUser().getId(),
                    project.getRepoFullName(),
                    project.getDefaultBranch(),
                    project.getRootDirectory(),
                    project.getSubdomain());
        });
    }

    /**
     * Marks the deployment READY and makes it the project's live one, unsetting
     * whichever deployment was live before. The previous one is cleared and
     * flushed first so the "one current per project" unique index is never
     * momentarily violated.
     */
    @Transactional
    public void markReadyAndCurrent(Long deploymentId) {
        Deployment deployment = deploymentRepository.findById(deploymentId).orElse(null);
        if (deployment == null) {
            return;
        }
        Long projectId = deployment.getProject().getId();

        deploymentRepository.findByProjectIdAndCurrentTrue(projectId)
                .filter(previous -> !previous.getId().equals(deploymentId))
                .ifPresent(previous -> previous.setCurrent(false));
        deploymentRepository.flush();

        deployment.setStatus(DeploymentStatus.READY);
        deployment.setCurrent(true);
        deployment.setReadyAt(Instant.now());
    }

    /**
     * Records what the build detected: the framework (always), and the folder the app
     * was found in — but the folder only fills the project's root directory when the
     * user hasn't set one, so an explicit choice is never overwritten.
     */
    @Transactional
    public void recordDetection(Long deploymentId, String framework, String detectedRootDirectory) {
        deploymentRepository.findById(deploymentId).ifPresent(deployment -> {
            Project project = deployment.getProject();
            if (framework != null) {
                project.setFramework(framework);
            }
            boolean rootUnset = project.getRootDirectory() == null || project.getRootDirectory().isBlank();
            if (rootUnset && detectedRootDirectory != null && !detectedRootDirectory.isBlank()) {
                project.setRootDirectory(detectedRootDirectory);
            }
        });
    }

    @Transactional
    public void markFailed(Long deploymentId, String message) {
        deploymentRepository.findById(deploymentId).ifPresent(deployment -> {
            deployment.setStatus(DeploymentStatus.FAILED);
            deployment.setErrorMessage(truncate(message));
        });
    }

    private String truncate(String message) {
        if (message == null || message.isBlank()) {
            return "Build failed";
        }
        return message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message;
    }
}
