package contractor.site.dmq.dtos;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload for deleting several images of one project at once. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectImageIdsRequest {

    @NotEmpty(message = "Select at least one image")
    private List<Long> ids;
}
