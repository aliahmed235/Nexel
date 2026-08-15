package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.Repositories.DeploymentRepository;
import com.aliahmed.Vercel.Repositories.ProjectRepository;
import com.aliahmed.Vercel.config.AppProperties;
import com.aliahmed.Vercel.dto.CommitResponse;
import com.aliahmed.Vercel.dto.CreateProjectRequest;
import com.aliahmed.Vercel.dto.GithubRepoResponse;
import com.aliahmed.Vercel.dto.ProjectResponse;
import com.aliahmed.Vercel.dto.UpdateProjectRequest;
import com.aliahmed.Vercel.entity.Project;
import com.aliahmed.Vercel.entity.User;
import com.aliahmed.Vercel.exception.ConflictException;
import com.aliahmed.Vercel.exception.ResourceNotFoundException;
import com.aliahmed.Vercel.mapper.ProjectMapper;
import com.aliahmed.Vercel.util.ProjectPaths;
import com.aliahmed.Vercel.util.Subdomains;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);
    private static final int SUFFIX_LENGTH = 4;
    private static final int MAX_SUBDOMAIN_ATTEMPTS = 5;

    private final ProjectRepository projectRepository;
    private final DeploymentRepository deploymentRepository;
    private final GithubRepoClient githubRepoClient;
    private final UserService userService;
    private final ProjectMapper projectMapper;
    private final StorageService storageService;
    private final AppProperties properties;

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
                .rootDirectory(request.rootDirectory())
                .defaultPath(request.defaultPath())
                .build();

        Project saved = projectRepository.save(project);
        registerWebhook(userId, saved);
        return projectMapper.toResponse(saved);
    }

    /**
     * Registers a GitHub push webhook so pushes auto-deploy. Best-effort: if it fails (or
     * webhooks aren't configured), the project still connects — it just won't auto-deploy.
     */
    private void registerWebhook(Long userId, Project project) {
        String secret = properties.getWebhook().getSecret();
        if (secret == null || secret.isBlank()) {
            return;
        }
        try {
            Long hookId = githubRepoClient.createPushWebhook(
                    userId, project.getRepoFullName(), properties.webhookCallbackUrl(), secret);
            project.setGithubHookId(hookId);
        } catch (RuntimeException e) {
            log.warn("Could not register webhook for {}: {}", project.getRepoFullName(), e.getMessage());
        }
    }

    /**
     * Updates a project's build settings so an already-connected repo can be pointed at
     * a subfolder, or given a landing path, without reconnecting. Only the fields present
     * in the request are touched; a blank value clears that setting.
     */
    @Transactional
    public ProjectResponse updateSettings(Long userId, Long id, UpdateProjectRequest request) {
        Project project = getOwned(userId, id);
        if (request.rootDirectory() != null) {
            project.setRootDirectory(ProjectPaths.normalizeRootDirectory(request.rootDirectory()));
        }
        if (request.defaultPath() != null) {
            project.setDefaultPath(ProjectPaths.normalizeDefaultPath(request.defaultPath()));
        }
        return projectMapper.toResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(Long userId) {
        return projectRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    /** Only the user's projects that have been deployed at least once. */
    @Transactional(readOnly = true)
    public List<ProjectResponse> listDeployed(Long userId) {
        return projectRepository.findDeployedByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long userId, Long id) {
        return projectMapper.toResponse(getOwned(userId, id));
    }

    /** The project's recent commits (deploy history), read live from GitHub. */
    @Transactional(readOnly = true)
    public List<CommitResponse> listCommits(Long userId, Long id, int limit) {
        Project project = getOwned(userId, id);
        return githubRepoClient.listCommits(userId, project.getRepoFullName(), project.getDefaultBranch(), limit);
    }

    /**
     * Disconnects a project: removes it and its deployments (the DB cascade),
     * then deletes the built site files those deployments left on disk so a
     * disconnect doesn't leave orphaned folders behind.
     */
    @Transactional
    public void delete(Long userId, Long id) {
        Project project = getOwned(userId, id);

        // Remove the GitHub webhook so pushes stop hitting us. Best-effort.
        if (project.getGithubHookId() != null) {
            try {
                githubRepoClient.deleteWebhook(userId, project.getRepoFullName(), project.getGithubHookId());
            } catch (RuntimeException e) {
                log.warn("Could not delete webhook for {}: {}", project.getRepoFullName(), e.getMessage());
            }
        }

        // Ids only (a scalar projection) — loading Deployment entities here would leave
        // them managed and pointing at the about-to-be-removed project, which fails the
        // Hibernate flush. The DB's ON DELETE CASCADE removes the rows themselves.
        List<Long> deploymentIds = deploymentRepository.findIdsByProjectId(id);

        projectRepository.delete(project);

        // Best-effort: a storage error must not fail the disconnect or leave the
        // project half-deleted. Orphaned files can be cleaned up later.
        deploymentIds.forEach(this::deleteStoredSite);
    }

    private void deleteStoredSite(Long deploymentId) {
        try {
            storageService.delete(deploymentId);
        } catch (RuntimeException e) {
            log.warn("Could not delete stored files for deployment {}: {}", deploymentId, e.getMessage());
        }
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
