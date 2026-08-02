package com.aliahmed.Vercel.Controllers;

import com.aliahmed.Vercel.Services.ProjectService;
import com.aliahmed.Vercel.dto.GithubRepoResponse;
import com.aliahmed.Vercel.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
public class GithubController {

    private final ProjectService projectService;

    @GetMapping("/repos")
    public ResponseEntity<List<GithubRepoResponse>> repos(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(projectService.listRepos(user.getId()));
    }
}
