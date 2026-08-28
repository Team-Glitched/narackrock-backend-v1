package glitched.adlips.application.project.port.out;

import glitched.adlips.domain.project.ProjectMember;
import java.util.Optional;

public interface ProjectMemberRepositoryPort {

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    ProjectMember save(ProjectMember member);
}
