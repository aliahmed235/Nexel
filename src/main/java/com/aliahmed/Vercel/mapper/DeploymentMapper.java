package com.aliahmed.Vercel.mapper;

import com.aliahmed.Vercel.dto.DeploymentResponse;
import com.aliahmed.Vercel.entity.Deployment;
import org.springframework.stereotype.Component;

@Component
public class DeploymentMapper {

    public DeploymentResponse toResponse(Deployment deployment) {
        return new DeploymentResponse(
                deployment.getId(),
                deployment.getProject().getId(),
                deployment.getStatus().name(),
                deployment.getCommitSha(),
                deployment.isCurrent(),
                deployment.getErrorMessage(),
                deployment.getCreatedAt(),
                deployment.getReadyAt());
    }
}
