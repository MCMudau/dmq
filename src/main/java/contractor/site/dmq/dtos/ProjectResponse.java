package contractor.site.dmq.dtos;

import java.time.LocalDateTime;
import java.util.List;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {

    private Long id;
    private String name;
    private String description;
    private Integer year;
    private Long serviceId;
    private String serviceName;
    private List<ProjectImageResponse> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}