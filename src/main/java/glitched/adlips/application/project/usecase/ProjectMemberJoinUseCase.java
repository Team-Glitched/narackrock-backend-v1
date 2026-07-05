package glitched.adlips.application.project.usecase;

import glitched.adlips.adapter.out.persistence.project.ProjectJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectMemberJpaRepository;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectMemberJoinUseCase {

    private final ProjectJpaRepository projects;
    private final ProjectMemberJpaRepository members;
    private final UserRepositoryPort users;

    public ProjectMemberJoinUseCase(
            ProjectJpaRepository projects,
            ProjectMemberJpaRepository members,
            UserRepositoryPort users
    ) {
        this.projects = projects;
        this.members = members;
        this.users = users;
    }

    @Transactional
    public void execute(Long projectId, Long userId) {
        if (members.findByProjectIdAndUserId(projectId, userId).isPresent()) {
            return;
        }
        Project project = projects.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_NOT_FOUND, "존재하지 않거나 삭제된 프로젝트입니다."));
        User user = users.findById(userId).filter(User::isActive)
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "참여할 권한이 없습니다."));

        members.save(new ProjectMember(project, user, ProjectMemberRole.EDITOR));
    }

    private ProjectApplicationException error(ProjectErrorCode code, String message) {
        return new ProjectApplicationException(code, message);
    }
}
