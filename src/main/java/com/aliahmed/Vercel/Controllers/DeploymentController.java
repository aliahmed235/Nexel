package com.aliahmed.Vercel.Controllers;

import com.aliahmed.Vercel.Services.DeploymentService;
import com.aliahmed.Vercel.dto.DeploymentResponse;
import com.aliahmed.Vercel.dto.TriggerDeploymentRequest;
import com.aliahmed.Vercel.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /** The "Deploy" action. Records a QUEUED build; an optional commit pins it to that SHA. */
    @PostMapping
    public ResponseEntity<DeploymentResponse> trigger(@PathVariable Long projectId,
                                                      @RequestBody(required = false) TriggerDeploymentRequest request,
                                                      @AuthenticationPrincipal User user) {
        String commit = request == null ? null : request.commit();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deploymentService.trigger(user.getId(), projectId, commit));
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

    /** Rollback/promote: make this (READY) deployment the project's live one. */
    @PostMapping("/{deploymentId}/promote")
    public ResponseEntity<DeploymentResponse> promote(@PathVariable Long projectId,
                                                      @PathVariable Long deploymentId,
                                                      @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(deploymentService.promote(user.getId(), projectId, deploymentId));
    }

    /** The captured build output for a deployment, as plain text. */
    @GetMapping(value = "/{deploymentId}/logs", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> logs(@PathVariable Long projectId,
                                       @PathVariable Long deploymentId,
                                       @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(deploymentService.logs(user.getId(), projectId, deploymentId));
    }
}
