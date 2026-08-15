package com.aliahmed.Vercel.Controllers;

import com.aliahmed.Vercel.Services.ProjectService;
import com.aliahmed.Vercel.dto.CommitResponse;
import com.aliahmed.Vercel.dto.CreateProjectRequest;
import com.aliahmed.Vercel.dto.ProjectResponse;
import com.aliahmed.Vercel.dto.UpdateProjectRequest;
import com.aliahmed.Vercel.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Connected projects. All endpoints are scoped to the authenticated user. */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@RequestBody CreateProjectRequest request,
                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.create(user.getId(), request));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> list(
            @RequestParam(name = "deployed", required = false, defaultValue = "false") boolean deployed,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(deployed
                ? projectService.listDeployed(user.getId())
                : projectService.list(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> get(@PathVariable Long id,
                                               @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(projectService.get(user.getId(), id));
    }

    /** The project's recent commits (deploy history) — pass a sha back as "commit" to deploy it. */
    @GetMapping("/{id}/commits")
    public ResponseEntity<List<CommitResponse>> commits(
            @PathVariable Long id,
            @RequestParam(name = "limit", required = false, defaultValue = "20") int limit,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(projectService.listCommits(user.getId(), id, limit));
    }

    /** Update build settings — e.g. point the project at a subfolder to build. */
    @PatchMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(@PathVariable Long id,
                                                  @RequestBody UpdateProjectRequest request,
                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(projectService.updateSettings(user.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal User user) {
        projectService.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
