package com.aliahmed.Vercel.mapper;

import com.aliahmed.Vercel.config.AppProperties;
import com.aliahmed.Vercel.dto.DeploymentResponse;
import com.aliahmed.Vercel.entity.Deployment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeploymentMapper {

    private final AppProperties properties;

    public DeploymentResponse toResponse(Deployment deployment) {
        return new DeploymentResponse(
                deployment.getId(),
                deployment.getProject().getId(),
                deployment.getStatus().name(),
                deployment.getCommitSha(),
                deployment.isCurrent(),
                deployment.getErrorMessage(),
                properties.siteUrl(deployment.getProject().getSubdomain(), deployment.getProject().getDefaultPath()),
                properties.previewUrl(deployment.getId()),
                deployment.getCreatedAt(),
                deployment.getReadyAt());
    }
}
