package com.aliahmed.Vercel.Controllers;

import com.aliahmed.Vercel.Services.DeploymentService;
import com.aliahmed.Vercel.dto.DeploymentResponse;
import com.aliahmed.Vercel.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Deployments live under their project, so ownership of the project is checked
 * once for every operation.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentService deploymentService;

    /** The "Deploy" action. Records a QUEUED build. */
    @PostMapping
    public ResponseEntity<DeploymentResponse> trigger(@PathVariable Long projectId,
                                                      @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deploymentService.trigger(user.getId(), projectId));
    }

    @GetMapping
    public ResponseEntity<List<DeploymentResponse>> list(@PathVariable Long projectId,
                                                         @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(deploymentService.list(user.getId(), projectId));
    }

    @GetMapping("/{deploymentId}")
    public ResponseEntity<DeploymentResponse> get(@PathVariable Long projectId,
                                                  @PathVariable Long deploymentId,
                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(deploymentService.get(user.getId(), projectId, deploymentId));
    }
}
