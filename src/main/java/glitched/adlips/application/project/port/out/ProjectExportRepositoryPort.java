package glitched.adlips.application.project.port.out;

import glitched.adlips.domain.project.ProjectExport;
import java.util.List;
import java.util.Optional;

public interface ProjectExportRepositoryPort {

    Optional<ProjectExport> findExportByIdAndProjectId(Long id, Long projectId);

    Optional<ProjectExport> findExportByIdForUpdate(Long id);

    List<Long> findQueuedExportIds(int limit);

    ProjectExport save(ProjectExport projectExport);
}
