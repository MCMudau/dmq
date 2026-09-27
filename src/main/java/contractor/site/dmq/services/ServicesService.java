package contractor.site.dmq.services;

import contractor.site.dmq.dtos.ServiceRequest;
import contractor.site.dmq.dtos.ServiceResponse;
import contractor.site.dmq.models.Admin;
import contractor.site.dmq.models.Service;
import contractor.site.dmq.repos.AdminRepository;
import contractor.site.dmq.repos.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ServicesService {

    private final ServiceRepository serviceRepository;
    private final AdminRepository adminRepository;

    @Transactional(readOnly = true)
    public List<ServiceResponse> getPublicServices() {
        return serviceRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> getServicesForAdmin(String search) {
        List<Service> services = (search == null || search.isBlank())
                ? serviceRepository.findAllByOrderByNameAsc()
                : serviceRepository.findByNameContainingIgnoreCaseOrderByNameAsc(search.trim());
        return services
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ServiceResponse addService(String adminEmail, ServiceRequest request) {
        Admin admin = findAdmin(adminEmail);

        Service service = Service.builder()
                .createdBy(admin)
                .name(request.getName().trim())
                .description(request.getDescription().trim())
                .build();

        return toResponse(serviceRepository.save(service));
    }

    @Transactional
    public ServiceResponse updateService(Long serviceId, ServiceRequest request) {
        Service service = findService(serviceId);
        service.setName(request.getName().trim());
        service.setDescription(request.getDescription().trim());
        return toResponse(serviceRepository.save(service));
    }

    @Transactional
    public void deleteService(Long serviceId) {
        serviceRepository.delete(findService(serviceId));
    }

    private Service findService(Long serviceId) {
        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
    }

    private Admin findAdmin(String adminEmail) {
        return adminRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin session invalid"));
    }

    private ServiceResponse toResponse(Service service) {
        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .createdAt(service.getCreatedAt())
                .updatedAt(service.getUpdatedAt())
                .build();
    }
}