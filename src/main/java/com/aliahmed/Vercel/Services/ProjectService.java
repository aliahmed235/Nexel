package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.Repositories.ProjectRepository;
import com.aliahmed.Vercel.dto.CreateProjectRequest;
import com.aliahmed.Vercel.dto.GithubRepoResponse;
import com.aliahmed.Vercel.dto.ProjectResponse;
import com.aliahmed.Vercel.entity.Project;
import com.aliahmed.Vercel.entity.User;
import com.aliahmed.Vercel.exception.ConflictException;
import com.aliahmed.Vercel.exception.ResourceNotFoundException;
import com.aliahmed.Vercel.mapper.ProjectMapper;
import com.aliahmed.Vercel.util.Subdomains;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final int SUFFIX_LENGTH = 4;
    private static final int MAX_SUBDOMAIN_ATTEMPTS = 5;

    private final ProjectRepository projectRepository;
    private final GithubRepoClient githubRepoClient;
    private final UserService userService;
    private final ProjectMapper projectMapper;

    /** Live read-through to GitHub — nothing stored. */
    @Transactional(readOnly = true)
    public List<GithubRepoResponse> listRepos(Long userId) {
        return githubRepoClient.listRepos(userId);
    }

    /**
     * Connects a repository. GitHub is asked to confirm the repo exists and is
     * accessible, and is taken as the source of truth for the id and default
     * branch, so a crafted request cannot store bogus data.
     */
    @Transactional
    public ProjectResponse create(Long userId, CreateProjectRequest request) {
        GithubRepoResponse repo = githubRepoClient.getRepo(userId, request.repoFullName());

        if (projectRepository.existsByUserIdAndGithubRepoId(userId, repo.githubRepoId())) {
            throw new ConflictException("This repository is already connected.");
        }

        String branch = request.branch() != null ? request.branch() : repo.defaultBranch();
        User owner = userService.getById(userId);

        Project project = Project.builder()
                .user(owner)
                .githubRepoId(repo.githubRepoId())
                .repoFullName(repo.fullName())
                .defaultBranch(branch)
                .subdomain(generateUniqueSubdomain(repo.name()))
                .build();

        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(Long userId) {
        return projectRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long userId, Long id) {
        return projectMapper.toResponse(getOwned(userId, id));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        projectRepository.delete(getOwned(userId, id));
    }

    private Project getOwned(Long userId, Long id) {
        return projectRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("No project " + id + " for this user"));
    }

    private String generateUniqueSubdomain(String repoName) {
        String base = Subdomains.slugify(repoName);
        for (int attempt = 0; attempt < MAX_SUBDOMAIN_ATTEMPTS; attempt++) {
            String candidate = base + "-" + Subdomains.randomSuffix(SUFFIX_LENGTH);
            if (!projectRepository.existsBySubdomain(candidate)) {
                return candidate;
            }
        }
        // Vanishingly unlikely after five tries; widen the suffix and take it.
        return base + "-" + Subdomains.randomSuffix(SUFFIX_LENGTH + 4);
    }
}
