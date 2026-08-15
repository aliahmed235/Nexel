package com.aliahmed.Vercel.Controllers;

import com.aliahmed.Vercel.Services.SiteResolutionService;
import com.aliahmed.Vercel.Services.StorageService;
import com.aliahmed.Vercel.Services.StoredObject;
import com.aliahmed.Vercel.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Serves deployed static sites.
 *
 * <p>Addressed by path for now — {@code /sites/{subdomain}/...} — so it works on
 * the plain Railway URL without a custom domain. Swapping to real subdomains
 * ({@code {subdomain}.yourdomain.com}) later means resolving the subdomain from
 * the Host header instead of the path; everything below stays the same.
 *
 * <p>Public: deployed sites have no authentication.
 */
@RestController
@RequiredArgsConstructor
public class SiteController {

    private final SiteResolutionService siteResolution;
    private final StorageService storageService;

    /** The site root, e.g. /sites/portfolio-a1b2 → its index.html. */
    @GetMapping("/sites/{subdomain}")
    public ResponseEntity<Resource> siteRoot(@PathVariable String subdomain) {
        return serve(subdomain, "");
    }

    /** Any path within the site, e.g. /sites/portfolio-a1b2/assets/app.js. */
    @GetMapping("/sites/{subdomain}/**")
    public ResponseEntity<Resource> siteFile(@PathVariable String subdomain, HttpServletRequest request) {
        return serve(subdomain, extractPathAfter(request, "/sites/" + subdomain));
    }

    /** A specific deployment's preview root, e.g. /d/42 → deployment 42's index.html. */
    @GetMapping("/d/{deploymentId}")
    public ResponseEntity<Resource> deploymentRoot(@PathVariable Long deploymentId) {
        return serveDeployment(deploymentId, "");
    }

    /** Any path within a specific deployment, e.g. /d/42/assets/app.js. */
    @GetMapping("/d/{deploymentId}/**")
    public ResponseEntity<Resource> deploymentFile(@PathVariable Long deploymentId, HttpServletRequest request) {
        return serveDeployment(deploymentId, extractPathAfter(request, "/d/" + deploymentId));
    }

    private ResponseEntity<Resource> serve(String subdomain, String path) {
        Long deploymentId = siteResolution.currentDeploymentId(subdomain)
                .orElseThrow(() -> new ResourceNotFoundException("No live site for '" + subdomain + "'"));
        return respond(deploymentId, path);
    }

    private ResponseEntity<Resource> serveDeployment(Long deploymentId, String path) {
        return respond(deploymentId, path);
    }

    private ResponseEntity<Resource> respond(Long deploymentId, String path) {
        StoredObject object = storageService.resolve(deploymentId, path)
                .orElseThrow(() -> new ResourceNotFoundException("Not found: " + path));

        return ResponseEntity.ok()
                .contentType(object.contentType())
                // no-cache for now so a redeploy shows immediately; real caching is phase 6.
                .cacheControl(CacheControl.noCache())
                .body(object.resource());
    }

    /** The part of the URL after the given prefix, decoded. */
    private String extractPathAfter(HttpServletRequest request, String prefix) {
        String uri = request.getRequestURI();
        int index = uri.indexOf(prefix);
        String rest = index >= 0 ? uri.substring(index + prefix.length()) : "";
        return URLDecoder.decode(rest, StandardCharsets.UTF_8);
    }
}
