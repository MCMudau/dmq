package contractor.site.dmq.repos;

import contractor.site.dmq.models.ProjectImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectImageRepository extends JpaRepository<ProjectImage, Long> {

    List<ProjectImage> findByProjectIdOrderByIdAsc(Long projectId);

    List<ProjectImage> findByProjectIdAndIdIn(Long projectId, List<Long> ids);
}
