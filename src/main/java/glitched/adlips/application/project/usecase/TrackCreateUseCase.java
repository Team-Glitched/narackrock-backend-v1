package glitched.adlips.application.project.usecase;

import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.TrackCreateRequest;
import glitched.adlips.application.project.dto.response.TrackCreateResponse;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectStatus;
import glitched.adlips.domain.project.InstrumentFamily;
import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.project.ProjectTrack;
import glitched.adlips.domain.user.User;
public class TrackCreateUseCase {
    private final ProjectRepositoryPort projects;
    private final ProjectMemberRepositoryPort members;
    private final ProjectTrackRepositoryPort tracks;
    private final UserRepositoryPort users;
    private final TransactionRunner transactionRunner;

    public TrackCreateUseCase(ProjectRepositoryPort projects, ProjectMemberRepositoryPort members,
                              ProjectTrackRepositoryPort tracks, UserRepositoryPort users) {
        this(projects, members, tracks, users, TransactionRunner.direct());
    }

    public TrackCreateUseCase(ProjectRepositoryPort projects, ProjectMemberRepositoryPort members,
                              ProjectTrackRepositoryPort tracks, UserRepositoryPort users,
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
        Project project = projects.findByIdAndDeletedAtIsNullForUpdate(request.projectId())
                .orElseThrow(() -> new ProjectApplicationException(ProjectErrorCode.PROJECT_NOT_FOUND, "존재하지 않는 프로젝트입니다."));
        var member = members.findByProjectIdAndUserId(request.projectId(), request.userId())
                .filter(it -> it.getRole() != ProjectMemberRole.VIEWER)
                .orElseThrow(() -> new ProjectApplicationException(ProjectErrorCode.PROJECT_ACCESS_DENIED, "트랙을 생성할 권한이 없습니다."));
        User user = users.findById(request.userId()).filter(User::isActive)
                .orElseThrow(() -> new ProjectApplicationException(ProjectErrorCode.PROJECT_ACCESS_DENIED, "트랙을 생성할 권한이 없습니다."));
        if (tracks.existsByProjectIdAndOwnerIdAndIsDeletedFalse(request.projectId(), request.userId())) {
            throw new ProjectApplicationException(ProjectErrorCode.TRACK_LIMIT_EXCEEDED,
                    "한 곡에는 사용자당 하나의 레이어만 만들 수 있습니다.");
        }
        if(project.getStatus()==ProjectStatus.COMPLETED) {
            throw new ProjectApplicationException(ProjectErrorCode.PROJECT_ACCESS_DENIED,"완료된 곡에는 레이어를 추가할 수 없습니다.");
        }
        String instrument=InstrumentFamily.normalize(request.instrument());
        var family=InstrumentFamily.of(instrument);
        if(family==null) throw new ProjectApplicationException(ProjectErrorCode.VALIDATION_ERROR,"악기나 보컬 역할을 선택해 주세요.");
        if(tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(request.projectId()).stream()
                .anyMatch(track -> track.getApprovalStatus()!=ApprovalStatus.REJECTED && InstrumentFamily.of(track.getInstrument())==family)) {
            throw new ProjectApplicationException(ProjectErrorCode.INSTRUMENT_FAMILY_TAKEN,family.label()+" 종류는 팀원이 이미 사용 중입니다. 다른 종류를 선택해 주세요.");
        }
        ProjectTrack saved = tracks.save(new ProjectTrack(project, user, request.name().trim(),
                instrument, request.sortOrder()));
        return new TrackCreateResponse(saved.getId(), project.getId(), saved.getName(), saved.getInstrument(),
                null, null, saved.getVolume(), saved.getPan(), saved.getSortOrder(), saved.isMuted(),
                saved.isSolo(), saved.getApprovalStatus());
    }

}
