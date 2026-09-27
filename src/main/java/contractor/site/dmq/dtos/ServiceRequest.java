package contractor.site.dmq.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for creating or editing a service. Only name and description are
 * admin-editable through this endpoint — basePrice/imageUrl/active on the
 * Service entity are left at their defaults for now.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest {

    @NotBlank(message = "Service name is required")
    private String name;

    @NotBlank(message = "Service description is required")
    private String description;
}
