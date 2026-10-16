package contractor.site.dmq.controllers;

import contractor.site.dmq.dtos.ProjectVideoResponse;
import contractor.site.dmq.services.ProjectVideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only video listing for the public landing page's project viewer.
 *
 * <ul>
 *   <li>GET /api/projects/{id}/videos — the project's videos, oldest first</li>
 * </ul>
 *
 * Lives outside /api/admin/** so visitors can reach it without a session; SecurityConfig
 * already permits /api/projects/**. Uploading and deleting stay in ProjectVideoController.
 */
@RestController
@RequiredArgsConstructor
public class PublicProjectVideoController {

    private final ProjectVideoService projectVideoService;

    @GetMapping("/api/projects/{id}/videos")
    public ResponseEntity<List<ProjectVideoResponse>> getVideos(@PathVariable("id") Long id) {
        return ResponseEntity.ok(projectVideoService.getVideos(id));
    }
}
