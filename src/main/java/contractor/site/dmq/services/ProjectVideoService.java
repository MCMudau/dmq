package contractor.site.dmq.services;

import contractor.site.dmq.dtos.ProjectVideoResponse;
import contractor.site.dmq.models.Project;
import contractor.site.dmq.models.ProjectVideo;
import contractor.site.dmq.repos.ProjectRepository;
import contractor.site.dmq.repos.ProjectVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for project videos. Independent of ProjectService: it only reads
 * projects to check they exist, and owns everything about videos (rows + files).
 */
@Component
@RequiredArgsConstructor
public class ProjectVideoService {

    private final ProjectRepository projectRepository;
    private final ProjectVideoRepository projectVideoRepository;
    private final VideoStorageService videoStorage;

    @Transactional(readOnly = true)
    public List<ProjectVideoResponse> getVideos(Long projectId) {
        findProject(projectId);
        return toResponses(projectVideoRepository.findByProjectIdOrderByIdAsc(projectId));
    }

    /** Adds one or many videos to a project and returns the project's full video list. */
    @Transactional
    public List<ProjectVideoResponse> addVideos(Long projectId, List<MultipartFile> files) {
        Project project = findProject(projectId);
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose at least one video to upload.");
        }

        List<String> storedUrls = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                storedUrls.add(videoStorage.storeVideo(file, "projects/" + projectId));
            }
            for (String url : storedUrls) {
                projectVideoRepository.save(ProjectVideo.builder().project(project).videoUrl(url).build());
            }
        } catch (RuntimeException e) {
            storedUrls.forEach(videoStorage::delete);   // all-or-nothing: don't leave orphan files
            throw e;
        }
        return toResponses(projectVideoRepository.findByProjectIdOrderByIdAsc(projectId));
    }

    /** Deletes one or many videos from a project and returns the project's remaining videos. */
    @Transactional
    public List<ProjectVideoResponse> deleteVideos(Long projectId, List<Long> videoIds) {
        findProject(projectId);
        if (videoIds == null || videoIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one video.");
        }

        List<ProjectVideo> toDelete = projectVideoRepository.findByProjectIdAndIdIn(projectId, videoIds);
        if (toDelete.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found for this project");
        }

        List<String> urls = toDelete.stream().map(ProjectVideo::getVideoUrl).toList();
        projectVideoRepository.deleteAll(toDelete);
        projectVideoRepository.flush();
        urls.forEach(videoStorage::delete);
        return toResponses(projectVideoRepository.findByProjectIdOrderByIdAsc(projectId));
    }

    // ---- Helpers ----

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private List<ProjectVideoResponse> toResponses(List<ProjectVideo> videos) {
        return videos.stream()
                .map(v -> ProjectVideoResponse.builder()
                        .id(v.getId())
                        .videoUrl(v.getVideoUrl())
                        .uploadedAt(v.getUploadedAt())
                        .build())
                .toList();
    }
}
