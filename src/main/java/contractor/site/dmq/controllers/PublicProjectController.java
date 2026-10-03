package contractor.site.dmq.controllers;

import contractor.site.dmq.dtos.ProjectResponse;
import contractor.site.dmq.services.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only project listing for the public landing page (index.html, "Our Previous Projects").
 *
 * <ul>
 *   <li>GET /api/projects — every project with its images, newest first</li>
 * </ul>
 *
 * Lives outside /api/admin/** so visitors can reach it without a session; SecurityConfig
 * already permits /api/projects. Creating, editing and deleting stay in ProjectController.
 */
@RestController
@RequiredArgsConstructor
public class PublicProjectController {

    private final ProjectService projectService;

    @GetMapping("/api/projects")
    public ResponseEntity<List<ProjectResponse>> getProjects() {
        return ResponseEntity.ok(projectService.getProjects());
    }
}
