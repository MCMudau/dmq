package contractor.site.dmq.dtos;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload for deleting several videos of one project at once. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectVideoIdsRequest {

    @NotEmpty(message = "Select at least one video")
    private List<Long> ids;
}
