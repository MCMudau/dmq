package contractor.site.dmq.repos;

import contractor.site.dmq.models.Project;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    /** Newest first, images fetched in the same query to avoid N+1. */
    @EntityGraph(attributePaths = {"images", "service"})
    List<Project> findAllByOrderByCreatedAtDesc();
}
