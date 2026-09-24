package contractor.site.dmq.repos;

import contractor.site.dmq.models.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
