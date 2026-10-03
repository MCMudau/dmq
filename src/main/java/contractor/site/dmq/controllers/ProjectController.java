package contractor.site.dmq.controllers;

import contractor.site.dmq.dtos.ProjectImageIdsRequest;
import contractor.site.dmq.dtos.ProjectRequest;
import contractor.site.dmq.dtos.ProjectResponse;
import contractor.site.dmq.services.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Admin endpoints for managing projects and their images. Everything under
 * /api/admin/** is guarded by AdminSessionFilter.
 *
 * <ul>
 *   <li>GET    /api/admin/projects                       — list all projects (with images)</li>
 *   <li>POST   /api/admin/projects                       — create</li>
 *   <li>PUT    /api/admin/projects/{id}                  — edit name/description</li>
 *   <li>DELETE /api/admin/projects/{id}                  — delete project and its images</li>
 *   <li>POST   /api/admin/projects/{id}/images           — add images (multipart, field "files")</li>
 *   <li>DELETE /api/admin/projects/{id}/images/{imageId} — delete one image</li>
 *   <li>DELETE /api/admin/projects/{id}/images           — delete many images, body {"ids":[...]}</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/projects")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getProjects() {
        return ResponseEntity.ok(projectService.getProjects());
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable("id") Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProjectResponse> addImages(
            @PathVariable("id") Long id,
            @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.addImages(id, files));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<ProjectResponse> deleteImage(
            @PathVariable("id") Long id,
            @PathVariable("imageId") Long imageId) {
        return ResponseEntity.ok(projectService.deleteImages(id, List.of(imageId)));
    }

    @DeleteMapping("/{id}/images")
    public ResponseEntity<ProjectResponse> deleteImages(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProjectImageIdsRequest request) {
        return ResponseEntity.ok(projectService.deleteImages(id, request.getIds()));
    }
}
