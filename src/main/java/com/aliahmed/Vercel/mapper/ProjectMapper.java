package com.aliahmed.Vercel.mapper;

import com.aliahmed.Vercel.config.AppProperties;
import com.aliahmed.Vercel.dto.ProjectResponse;
import com.aliahmed.Vercel.entity.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Entity to DTO in one place, so a column added later doesn't silently appear
 * in the API response.
 */
@Component
@RequiredArgsConstructor
public class ProjectMapper {

    private final AppProperties properties;

    public ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getGithubRepoId(),
                project.getRepoFullName(),
                project.getDefaultBranch(),
                project.getSubdomain(),
                project.getFramework(),
                project.getRootDirectory(),
                properties.siteUrl(project.getSubdomain()),
                project.getCreatedAt());
    }
}
