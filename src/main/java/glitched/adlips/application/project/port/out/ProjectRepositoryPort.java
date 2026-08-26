package glitched.adlips.application.project.port.out;

import glitched.adlips.domain.project.Project;
import java.util.Optional;

public interface ProjectRepositoryPort {

    Optional<Project> findByIdAndDeletedAtIsNull(Long id);

    Optional<Project> findByIdAndDeletedAtIsNullForUpdate(Long id);

    Project save(Project project);
}
