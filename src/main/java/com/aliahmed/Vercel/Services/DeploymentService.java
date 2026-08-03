package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.Repositories.DeploymentRepository;
import com.aliahmed.Vercel.Repositories.ProjectRepository;
import com.aliahmed.Vercel.dto.DeploymentResponse;
import com.aliahmed.Vercel.entity.Deployment;
import com.aliahmed.Vercel.entity.DeploymentStatus;
import com.aliahmed.Vercel.entity.Project;
import com.aliahmed.Vercel.exception.ResourceNotFoundException;
import com.aliahmed.Vercel.mapper.DeploymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final ProjectRepository projectRepository;
    private final DeploymentMapper deploymentMapper;

    /**
     * Records a new build request for a project as {@link DeploymentStatus#QUEUED}.
     *
     * <p>Phase 3.2 will add a single line here — pushing the saved deployment's
     * id onto the Redis build queue. Until then the row simply sits QUEUED; no
     * worker exists to pick it up yet.
     */
    @Transactional
    public DeploymentResponse trigger(Long userId, Long projectId) {
        Project project = requireOwnedProject(userId, projectId);

        Deployment deployment = Deployment.builder()
                .project(project)
                .status(DeploymentStatus.QUEUED)
                .current(false)
                .build();

        Deployment saved = deploymentRepository.save(deployment);
        // TODO (phase 3.2): buildQueue.enqueue(saved.getId());
        return deploymentMapper.toResponse(saved);
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

    /** One place the "is this project mine?" check lives, reused by every method. */
    private Project requireOwnedProject(Long userId, Long projectId) {
        return projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("No project " + projectId + " for this user"));
    }
}
