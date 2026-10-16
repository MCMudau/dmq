package contractor.site.dmq.controllers;

import contractor.site.dmq.dtos.ProjectVideoIdsRequest;
import contractor.site.dmq.dtos.ProjectVideoResponse;
import contractor.site.dmq.services.ProjectVideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Admin endpoints for a project's videos, separate from the image endpoints.
 * Everything under /api/admin/** is guarded by AdminSessionFilter.
 *
 * <ul>
 *   <li>GET    /api/admin/projects/{id}/videos           — list the project's videos</li>
 *   <li>POST   /api/admin/projects/{id}/videos           — add videos (multipart, field "files")</li>
 *   <li>DELETE /api/admin/projects/{id}/videos/{videoId} — delete one video</li>
 *   <li>DELETE /api/admin/projects/{id}/videos           — delete many videos, body {"ids":[...]}</li>
 * </ul>
 * Each call returns the project's video list after the change.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/projects/{id}/videos")
public class ProjectVideoController {

    private final ProjectVideoService projectVideoService;

    @GetMapping
    public ResponseEntity<List<ProjectVideoResponse>> getVideos(@PathVariable("id") Long id) {
        return ResponseEntity.ok(projectVideoService.getVideos(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ProjectVideoResponse>> addVideos(
            @PathVariable("id") Long id,
            @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectVideoService.addVideos(id, files));
    }

    @DeleteMapping("/{videoId}")
    public ResponseEntity<List<ProjectVideoResponse>> deleteVideo(
            @PathVariable("id") Long id,
            @PathVariable("videoId") Long videoId) {
        return ResponseEntity.ok(projectVideoService.deleteVideos(id, List.of(videoId)));
    }

    @DeleteMapping
    public ResponseEntity<List<ProjectVideoResponse>> deleteVideos(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProjectVideoIdsRequest request) {
        return ResponseEntity.ok(projectVideoService.deleteVideos(id, request.getIds()));
    }
}
