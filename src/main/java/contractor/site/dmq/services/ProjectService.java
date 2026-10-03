package contractor.site.dmq.services;

import contractor.site.dmq.dtos.ProjectImageResponse;
import contractor.site.dmq.dtos.ProjectRequest;
import contractor.site.dmq.dtos.ProjectResponse;
import contractor.site.dmq.models.Project;
import contractor.site.dmq.models.ProjectImage;
import contractor.site.dmq.models.Service;
import contractor.site.dmq.repos.ServiceRepository;
import contractor.site.dmq.repos.ProjectImageRepository;
import contractor.site.dmq.repos.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for projects and their images. Image files live on disk (see
 * FileStorageService); the database only keeps the link.
 */
@Component
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectImageRepository projectImageRepository;
    private final ServiceRepository serviceRepository;
    private final FileStorageService fileStorage;

    // ---- Projects ----

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjects() {
        return projectRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        Project project = Project.builder()
                .name(request.getName().trim())
                .description(request.getDescription().trim())
                .year(request.getYear())
                .service(findService(request.getServiceId()))
                .build();
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse updateProject(Long projectId, ProjectRequest request) {
        Project project = findProject(projectId);
        project.setName(request.getName().trim());
        project.setDescription(request.getDescription().trim());
        project.setYear(request.getYear());
        project.setService(findService(request.getServiceId()));
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(Long projectId) {
        Project project = findProject(projectId);
        List<String> urls = project.getImages().stream().map(ProjectImage::getImageUrl).toList();
        projectRepository.delete(project);   // cascades to its image rows
        urls.forEach(fileStorage::delete);
    }

    // ---- Images ----

    /** Adds one or many images to a project and returns the refreshed project. */
    @Transactional
    public ProjectResponse addImages(Long projectId, List<MultipartFile> files) {
        Project project = findProject(projectId);
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose at least one image to upload.");
        }

        List<String> storedUrls = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                storedUrls.add(fileStorage.storeImage(file, "projects/" + projectId));
            }
        } catch (RuntimeException e) {
            storedUrls.forEach(fileStorage::delete);   // all-or-nothing: don't leave orphans
            throw e;
        }

        for (String url : storedUrls) {
            project.getImages().add(ProjectImage.builder().project(project).imageUrl(url).build());
        }
        return toResponse(projectRepository.save(project));
    }

    /** Deletes one or many images from a project and returns the refreshed project. */
    @Transactional
    public ProjectResponse deleteImages(Long projectId, List<Long> imageIds) {
        Project project = findProject(projectId);
        if (imageIds == null || imageIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one image.");
        }

        List<ProjectImage> toDelete = projectImageRepository.findByProjectIdAndIdIn(projectId, imageIds);
        if (toDelete.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found for this project");
        }

        List<String> urls = toDelete.stream().map(ProjectImage::getImageUrl).toList();
        project.getImages().removeIf(img -> imageIds.contains(img.getId()));   // orphanRemoval deletes rows
        Project saved = projectRepository.save(project);
        urls.forEach(fileStorage::delete);
        return toResponse(saved);
    }

    // ---- Helpers ----

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private Service findService(Long serviceId) {
        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "The selected service category no longer exists."));
    }

    private ProjectResponse toResponse(Project project) {
        List<ProjectImageResponse> images = project.getImages().stream()
                .map(img -> ProjectImageResponse.builder().id(img.getId()).imageUrl(img.getImageUrl()).build())
                .toList();
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .year(project.getYear())
                .serviceId(project.getService() == null ? null : project.getService().getId())
                .serviceName(project.getService() == null ? null : project.getService().getName())
                .images(images)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}