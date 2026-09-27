package contractor.site.dmq.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import contractor.site.dmq.dtos.ServiceRequest;
import contractor.site.dmq.dtos.ServiceResponse;
import contractor.site.dmq.services.ServicesService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Public + admin endpoints for managing DMQ's services.
 *
 * <ul>
 *   <li>GET    /api/services              — public, active services only (marketing site)</li>
 *   <li>GET    /api/admin/services         — admin, every service, optional ?search= by name</li>
 *   <li>POST   /api/admin/services         — admin, create a service</li>
 *   <li>PUT    /api/admin/services/{id}    — admin, edit name/description</li>
 *   <li>DELETE /api/admin/services/{id}    — admin, delete</li>
 * </ul>
 *
 * /api/admin/** is guarded by AdminSessionFilter (contractor.site.dmq.security), which
 * requires a valid "adminEmail" session attribute set at /api/admin/account/login — the
 * public GET is already allow-listed in SecurityConfig under "/api/services/**".
 */
@RestController
@RequiredArgsConstructor
public class ServicesController {

    private static final String SESSION_ADMIN_EMAIL = "adminEmail";

    private final ServicesService servicesService;

    // ---- Public ----

    @GetMapping("/api/services")
    public ResponseEntity<List<ServiceResponse>> getPublicServices() {
        return ResponseEntity.ok(servicesService.getPublicServices());
    }

    // ---- Admin ----

    @GetMapping("/api/admin/services")
    public ResponseEntity<List<ServiceResponse>> getServicesForAdmin(
            @RequestParam(value = "search", required = false) String search) {
        return ResponseEntity.ok(servicesService.getServicesForAdmin(search));
    }

    @PostMapping("/api/admin/services")
    public ResponseEntity<ServiceResponse> addService(
            @Valid @RequestBody ServiceRequest request,
            HttpSession session) {
        ServiceResponse created = servicesService.addService(requireAdminEmail(session), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/api/admin/services/{id}")
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable("id") Long id,
            @Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.ok(servicesService.updateService(id, request));
    }

    @DeleteMapping("/api/admin/services/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable("id") Long id) {
        servicesService.deleteService(id);
        return ResponseEntity.noContent().build();
    }

    private String requireAdminEmail(HttpSession session) {
        Object email = session.getAttribute(SESSION_ADMIN_EMAIL);
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in to continue.");
        }
        return email.toString();
    }
}
