package glitched.adlips.application.project.usecase;

import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.port.out.ProjectMemberJoinPort;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectStatus;
import glitched.adlips.domain.user.User;

public class ProjectMemberJoinUseCase {

    private final ProjectMemberJoinPort members;
    private final UserRepositoryPort users;

    public ProjectMemberJoinUseCase(
            ProjectMemberJoinPort members,
            UserRepositoryPort users
    ) {
        this.members = members;
        this.users = users;
    }

    public void execute(Long projectId, Long userId) {
        Project project = members.findActiveProjectForUpdate(projectId)
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_NOT_FOUND, "존재하지 않거나 삭제된 프로젝트입니다."));
        if (project.getStatus() == ProjectStatus.COMPLETED) {
            throw error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "완료된 프로젝트에는 참여할 수 없습니다.");
        }
        if (members.existsByProjectIdAndUserId(projectId, userId)) {
            return;
        }
        User user = users.findById(userId).filter(User::isActive)
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "참여할 권한이 없습니다."));

        members.saveEditor(project, user);
    }

    private ProjectApplicationException error(ProjectErrorCode code, String message) {
        return new ProjectApplicationException(code, message);
    }
}
