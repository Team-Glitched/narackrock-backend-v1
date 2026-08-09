package glitched.adlips.application.project.usecase;

import glitched.adlips.adapter.out.persistence.project.ProjectJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectMemberJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectTrackJpaRepository;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.TrackCreateRequest;
import glitched.adlips.application.project.dto.response.TrackCreateResponse;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.project.ProjectTrack;
import glitched.adlips.domain.user.User;
public class TrackCreateUseCase {
    private final ProjectJpaRepository projects;
    private final ProjectMemberJpaRepository members;
    private final ProjectTrackJpaRepository tracks;
    private final UserRepositoryPort users;
    private final TransactionRunner transactionRunner;

    public TrackCreateUseCase(ProjectJpaRepository projects, ProjectMemberJpaRepository members,
                              ProjectTrackJpaRepository tracks, UserRepositoryPort users) {
        this(projects, members, tracks, users, TransactionRunner.direct());
    }

    public TrackCreateUseCase(ProjectJpaRepository projects, ProjectMemberJpaRepository members,
                              ProjectTrackJpaRepository tracks, UserRepositoryPort users,
                              TransactionRunner transactionRunner) {
        this.projects = projects;
        this.members = members;
        this.tracks = tracks;
        this.users = users;
        this.transactionRunner = transactionRunner;
    }

    public TrackCreateResponse execute(TrackCreateRequest request) {
        return transactionRunner.required(() -> executeInternal(request));
    }

    private TrackCreateResponse executeInternal(TrackCreateRequest request) {
        if (request == null || request.name() == null || request.name().isBlank() || request.sortOrder() < 0) {
            throw new ProjectApplicationException(ProjectErrorCode.VALIDATION_ERROR, "트랙 이름과 순서를 확인해 주세요.");
        }
        Project project = projects.findByIdAndDeletedAtIsNull(request.projectId())
                .orElseThrow(() -> new ProjectApplicationException(ProjectErrorCode.PROJECT_NOT_FOUND, "존재하지 않는 프로젝트입니다."));
        var member = members.findByProjectIdAndUserId(request.projectId(), request.userId())
                .filter(it -> it.getRole() != ProjectMemberRole.VIEWER)
                .orElseThrow(() -> new ProjectApplicationException(ProjectErrorCode.PROJECT_ACCESS_DENIED, "트랙을 생성할 권한이 없습니다."));
        User user = users.findById(request.userId()).filter(User::isActive)
                .orElseThrow(() -> new ProjectApplicationException(ProjectErrorCode.PROJECT_ACCESS_DENIED, "트랙을 생성할 권한이 없습니다."));
        ProjectTrack saved = tracks.save(new ProjectTrack(project, user, request.name().trim(),
                normalize(request.instrument()), request.sortOrder()));
        return new TrackCreateResponse(saved.getId(), project.getId(), saved.getName(), saved.getInstrument(),
                null, null, saved.getVolume(), saved.getPan(), saved.getSortOrder(), saved.isMuted(),
                saved.isSolo(), saved.getApprovalStatus());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
