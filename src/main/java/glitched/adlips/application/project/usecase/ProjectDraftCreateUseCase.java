package glitched.adlips.application.project.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.response.ProjectCreateResponse;
import glitched.adlips.application.project.dto.response.ProjectVersionResponse;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.user.User;

public class ProjectDraftCreateUseCase {
    private final UserRepositoryPort users;
    private final ProjectRepositoryPort projects;
    private final ProjectMemberRepositoryPort members;
    private final TransactionRunner transactions;

    public ProjectDraftCreateUseCase(UserRepositoryPort users, ProjectRepositoryPort projects,
                                     ProjectMemberRepositoryPort members) {
        this(users, projects, members, TransactionRunner.direct());
    }

    public ProjectDraftCreateUseCase(UserRepositoryPort users, ProjectRepositoryPort projects,
                                     ProjectMemberRepositoryPort members, TransactionRunner transactions) {
        this.users = users;
        this.projects = projects;
        this.members = members;
        this.transactions = transactions;
    }

    public ProjectCreateResponse execute(Long ownerId) {
        return transactions.required(() -> {
            User owner = users.findById(ownerId).filter(User::isActive).orElseThrow(() ->
                    new ProjectApplicationException(ProjectErrorCode.PROJECT_ACCESS_DENIED, "프로젝트를 생성할 권한이 없습니다."));
            Project draft = projects.save(new Project(owner, Project.UNTITLED_DRAFT, null, 0L));
            members.save(new ProjectMember(draft, owner, ProjectMemberRole.OWNER));
            return new ProjectCreateResponse(draft.getId(), draft.getTitle(), draft.getDescription(),
                    draft.getAlbumImageFileId(), null, ownerId,
                    new ProjectVersionResponse(draft.getMajorVersion(), draft.getMinorVersion(), draft.getDisplayVersion()),
                    draft.getStatus(), draft.isPublic(), draft.getCreatedAt());
        });
    }
}
