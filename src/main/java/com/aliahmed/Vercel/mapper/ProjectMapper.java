package com.aliahmed.Vercel.mapper;

import com.aliahmed.Vercel.dto.ProjectResponse;
import com.aliahmed.Vercel.entity.Project;
import org.springframework.stereotype.Component;

/**
 * Entity to DTO in one place, so a column added later doesn't silently appear
 * in the API response.
 */
@Component
public class ProjectMapper {

    public ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getGithubRepoId(),
                project.getRepoFullName(),
                project.getDefaultBranch(),
                project.getSubdomain(),
                project.getFramework(),
                project.getCreatedAt());
    }
}
