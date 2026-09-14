package glitched.adlips.application.project.port.out;

import glitched.adlips.domain.project.ProjectExport;
import java.util.Optional;

public interface ProjectExportRepositoryPort {

    Optional<ProjectExport> findExportByIdAndProjectId(Long id, Long projectId);

    ProjectExport save(ProjectExport projectExport);
}
