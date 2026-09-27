package contractor.site.dmq.repos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import contractor.site.dmq.models.Service;

public interface ServiceRepository extends JpaRepository<Service, Long> {

      /** Public marketing-site listing: every service, alphabetical (no active/hidden flag on this entity). */
    List<Service> findAllByOrderByNameAsc();

    /**
     * Admin search: services whose name contains the text (case-insensitive), alphabetical.
     * Never pass null here (Postgres can't type a null LIKE parameter); for "no search"
     * call findAllByOrderByNameAsc() instead.
     */
    List<Service> findByNameContainingIgnoreCaseOrderByNameAsc(String name);
}
