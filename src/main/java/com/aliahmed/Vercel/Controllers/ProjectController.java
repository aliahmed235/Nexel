package com.aliahmed.Vercel.Controllers;

import com.aliahmed.Vercel.Services.ProjectService;
import com.aliahmed.Vercel.dto.CreateProjectRequest;
import com.aliahmed.Vercel.dto.ProjectResponse;
import com.aliahmed.Vercel.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public ResponseEntity<List<ProjectResponse>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(projectService.list(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> get(@PathVariable Long id,
                                               @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(projectService.get(user.getId(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal User user) {
        projectService.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
