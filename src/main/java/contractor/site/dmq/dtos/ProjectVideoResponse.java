package contractor.site.dmq.dtos;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectVideoResponse {

    private Long id;
    private String videoUrl;
    private LocalDateTime uploadedAt;
}
