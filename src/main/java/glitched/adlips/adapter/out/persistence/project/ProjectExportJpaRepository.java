package glitched.adlips.adapter.out.persistence.project;

import glitched.adlips.domain.project.ProjectExport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectExportJpaRepository extends JpaRepository<ProjectExport, Long> {
}
