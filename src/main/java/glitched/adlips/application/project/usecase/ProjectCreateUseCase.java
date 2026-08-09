package glitched.adlips.application.project.usecase;

import glitched.adlips.adapter.out.persistence.project.ProjectJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectMemberJpaRepository;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.ProjectCreateRequest;
import glitched.adlips.application.project.dto.response.ProjectCreateResponse;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.dto.response.ProjectVersionResponse;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.media.MediaFileStatus;
import glitched.adlips.domain.media.MediaFileType;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.user.User;
public class ProjectCreateUseCase {
    private final UserRepositoryPort userRepository;
    private final MediaFileRepositoryPort mediaFileRepository;
    private final ProjectJpaRepository projectRepository;
    private final ProjectMemberJpaRepository memberRepository;
    private final TransactionRunner transactionRunner;

    public ProjectCreateUseCase(UserRepositoryPort userRepository,
                                MediaFileRepositoryPort mediaFileRepository,
                                ProjectJpaRepository projectRepository,
                                ProjectMemberJpaRepository memberRepository) {
        this(userRepository, mediaFileRepository, projectRepository, memberRepository, TransactionRunner.direct());
    }

    public ProjectCreateUseCase(UserRepositoryPort userRepository,
                                MediaFileRepositoryPort mediaFileRepository,
                                ProjectJpaRepository projectRepository,
                                ProjectMemberJpaRepository memberRepository,
                                TransactionRunner transactionRunner) {
        this.userRepository = userRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.transactionRunner = transactionRunner;
    }

    public ProjectCreateResponse execute(ProjectCreateRequest request) {
        return transactionRunner.required(() -> executeInternal(request));
    }

    private ProjectCreateResponse executeInternal(ProjectCreateRequest request) {
        if (request == null || request.ownerId() == null || request.albumImageFileId() == null
                || request.title() == null || request.title().isBlank()) {
            throw error(ProjectErrorCode.VALIDATION_ERROR, "곡 이름과 앨범 이미지는 필수입니다.");
        }
        User owner = userRepository.findById(request.ownerId()).filter(User::isActive)
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "프로젝트를 생성할 권한이 없습니다."));
        MediaFile image = mediaFileRepository.findById(request.albumImageFileId())
                .orElseThrow(() -> error(ProjectErrorCode.MEDIA_FILE_NOT_FOUND, "존재하지 않는 미디어 파일입니다."));
        if (!owner.getId().equals(image.getOwnerId())) {
            throw error(ProjectErrorCode.MEDIA_FILE_ACCESS_DENIED, "해당 미디어 파일을 사용할 권한이 없습니다.");
        }
        if (image.getFileType() != MediaFileType.IMAGE) {
            throw error(ProjectErrorCode.INVALID_ALBUM_IMAGE, "사용할 수 없는 앨범 표지 이미지입니다.");
        }
        if (image.getStatus() != MediaFileStatus.READY) {
            throw error(ProjectErrorCode.MEDIA_FILE_NOT_READY, "앨범 이미지 파일의 업로드 처리가 완료되지 않았습니다.");
        }
        Project saved = projectRepository.save(new Project(owner, request.title().trim(), request.description(), image.getId()));
        memberRepository.save(new ProjectMember(saved, owner, ProjectMemberRole.OWNER));
        return new ProjectCreateResponse(saved.getId(), saved.getTitle(), saved.getDescription(), image.getId(),
                image.getFileUrl(), owner.getId(),
                new ProjectVersionResponse(saved.getMajorVersion(), saved.getMinorVersion(), saved.getDisplayVersion()),
                saved.getStatus(), saved.isPublic(), saved.getCreatedAt());
    }

    private ProjectApplicationException error(ProjectErrorCode code, String message) {
        return new ProjectApplicationException(code, message);
    }
}
