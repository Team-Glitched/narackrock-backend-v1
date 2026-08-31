package glitched.adlips.application.project.port.out;

import glitched.adlips.domain.project.ProjectExport;

public interface ProjectExportRepositoryPort {

    ProjectExport save(ProjectExport projectExport);
}
