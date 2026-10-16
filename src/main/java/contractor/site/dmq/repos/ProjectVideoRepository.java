package contractor.site.dmq.repos;

import contractor.site.dmq.models.ProjectVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectVideoRepository extends JpaRepository<ProjectVideo, Long> {

    List<ProjectVideo> findByProjectIdOrderByIdAsc(Long projectId);

    List<ProjectVideo> findByProjectIdAndIdIn(Long projectId, List<Long> ids);
}
