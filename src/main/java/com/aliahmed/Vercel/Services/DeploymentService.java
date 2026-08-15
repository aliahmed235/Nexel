package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.Repositories.DeploymentRepository;
import com.aliahmed.Vercel.Repositories.ProjectRepository;
import com.aliahmed.Vercel.dto.DeploymentResponse;
import com.aliahmed.Vercel.entity.Deployment;
import com.aliahmed.Vercel.entity.DeploymentStatus;
import com.aliahmed.Vercel.entity.Project;
import com.aliahmed.Vercel.exception.ConflictException;
import com.aliahmed.Vercel.exception.ResourceNotFoundException;
import com.aliahmed.Vercel.mapper.DeploymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final ProjectRepository projectRepository;
    private final DeploymentMapper deploymentMapper;
    private final BuildQueue buildQueue;
    private final DeploymentStatusService deploymentStatusService;
    private final StorageService storageService;

    /**
     * Records a new build request as {@link DeploymentStatus#QUEUED} and pushes
     * its id onto the build queue for a worker to pick up.
     *
     * <p>The enqueue is deferred to <em>after</em> the transaction commits. The
     * worker is a separate thread whose blocking pop wakes the instant an id is
     * pushed; enqueuing before commit let it look the deployment up before the
     * row was visible, so it was skipped as a phantom and never built. Waiting
     * for commit also means a rollback never leaves a queued id behind.
     */
    @Transactional
    public DeploymentResponse trigger(Long userId, Long projectId, String commit) {
        Project project = requireOwnedProject(userId, projectId);
        return deploymentMapper.toResponse(createDeployment(project, normalizeCommit(commit)));
    }

    /**
     * Creates a QUEUED deployment for a push, bypassing the ownership check — the caller
     * (the webhook) has already authenticated the request via its HMAC signature.
     */
    @Transactional
    public void deployOnPush(Project project, String commit) {
        createDeployment(project, normalizeCommit(commit));
    }

    private Deployment createDeployment(Project project, String commit) {
        Deployment deployment = Deployment.builder()
                .project(project)
                .status(DeploymentStatus.QUEUED)
                .current(false)
                .commitSha(commit)
                .build();

        Deployment saved = deploymentRepository.save(deployment);
        enqueueAfterCommit(saved.getId());
        return saved;
    }

    private String normalizeCommit(String commit) {
        return (commit == null || commit.isBlank()) ? null : commit.trim();
    }

    private void enqueueAfterCommit(Long deploymentId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                buildQueue.enqueue(deploymentId);
            }
        });
    }

    @Transactional(readOnly = true)
    public List<DeploymentResponse> list(Long userId, Long projectId) {
        requireOwnedProject(userId, projectId);
        return deploymentRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(deploymentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeploymentResponse get(Long userId, Long projectId, Long deploymentId) {
        requireOwnedProject(userId, projectId);
        Deployment deployment = deploymentRepository.findByIdAndProjectId(deploymentId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No deployment " + deploymentId + " for project " + projectId));
        return deploymentMapper.toResponse(deployment);
    }

    /** Rollback/promote: make a past READY deployment the project's live one — no rebuild. */
    @Transactional
    public DeploymentResponse promote(Long userId, Long projectId, Long deploymentId) {
        requireOwnedProject(userId, projectId);
        Deployment deployment = deploymentRepository.findByIdAndProjectId(deploymentId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No deployment " + deploymentId + " for project " + projectId));
        if (deployment.getStatus() != DeploymentStatus.READY) {
            throw new ConflictException(
                    "Only a READY deployment can be promoted; this one is " + deployment.getStatus());
        }
        deploymentStatusService.makeCurrent(deployment);
        return deploymentMapper.toResponse(deployment);
    }

    /** The captured build output for one deployment (empty string if none was stored). */
    @Transactional(readOnly = true)
    public String logs(Long userId, Long projectId, Long deploymentId) {
        requireOwnedProject(userId, projectId);
        deploymentRepository.findByIdAndProjectId(deploymentId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No deployment " + deploymentId + " for project " + projectId));
        return storageService.readLog(deploymentId).orElse("");
    }

    /** One place the "is this project mine?" check lives, reused by every method. */
    private Project requireOwnedProject(Long userId, Long projectId) {
        return projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("No project " + projectId + " for this user"));
    }
}
