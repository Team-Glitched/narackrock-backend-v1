package glitched.adlips.application.project.port.out;

import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.user.User;
import java.util.Optional;

public interface ProjectMemberJoinPort {

    Optional<Project> findActiveProjectForUpdate(Long projectId);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    void saveEditor(Project project, User user);
}
