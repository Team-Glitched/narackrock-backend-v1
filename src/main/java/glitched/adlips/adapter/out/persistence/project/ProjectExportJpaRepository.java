package glitched.adlips.adapter.out.persistence.project;

import glitched.adlips.domain.project.ProjectExport;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectExportJpaRepository extends JpaRepository<ProjectExport, Long> {

    Optional<ProjectExport> findExportByIdAndProjectId(Long id, Long projectId);
}
