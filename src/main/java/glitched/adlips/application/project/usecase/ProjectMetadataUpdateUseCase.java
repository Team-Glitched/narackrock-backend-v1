package glitched.adlips.application.project.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.media.MediaFileStatus;
import glitched.adlips.domain.media.MediaFileType;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.project.ProjectStatus;

public class ProjectMetadataUpdateUseCase {
    private final ProjectRepositoryPort projects;
    private final ProjectMemberRepositoryPort members;
    private final MediaFileRepositoryPort mediaFiles;
    private final TransactionRunner transactions;

    public ProjectMetadataUpdateUseCase(ProjectRepositoryPort projects, ProjectMemberRepositoryPort members,
                                        MediaFileRepositoryPort mediaFiles) {
        this(projects, members, mediaFiles, TransactionRunner.direct());
    }

    public ProjectMetadataUpdateUseCase(ProjectRepositoryPort projects, ProjectMemberRepositoryPort members,
                                        MediaFileRepositoryPort mediaFiles, TransactionRunner transactions) {
        this.projects = projects;
        this.members = members;
        this.mediaFiles = mediaFiles;
        this.transactions = transactions;
    }

    public void execute(Long projectId, Long userId, String title, Long albumImageFileId) {
        transactions.required(() -> {
            if (title == null || title.isBlank() || albumImageFileId == null || albumImageFileId <= 0) {
                throw error(ProjectErrorCode.VALIDATION_ERROR, "곡 이름과 앨범 이미지는 필수입니다.");
            }
            var project = projects.findByIdAndDeletedAtIsNull(projectId)
                    .orElseThrow(() -> error(ProjectErrorCode.PROJECT_NOT_FOUND, "존재하지 않는 프로젝트입니다."));
            members.findByProjectIdAndUserId(projectId, userId)
                    .filter(member -> member.getRole() == ProjectMemberRole.OWNER)
                    .orElseThrow(() -> error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "곡 정보를 변경할 권한이 없습니다."));
            if (project.getStatus() != ProjectStatus.DRAFT) {
                throw error(ProjectErrorCode.VALIDATION_ERROR, "초안의 곡 정보만 변경할 수 있습니다.");
            }
            MediaFile image = mediaFiles.findById(albumImageFileId)
                    .orElseThrow(() -> error(ProjectErrorCode.MEDIA_FILE_NOT_FOUND, "존재하지 않는 미디어 파일입니다."));
            if (!userId.equals(image.getOwnerId())) {
                throw error(ProjectErrorCode.MEDIA_FILE_ACCESS_DENIED, "해당 미디어 파일을 사용할 권한이 없습니다.");
            }
            if (image.getFileType() != MediaFileType.IMAGE) {
                throw error(ProjectErrorCode.INVALID_ALBUM_IMAGE, "사용할 수 없는 앨범 표지 이미지입니다.");
            }
            if (image.getStatus() != MediaFileStatus.READY) {
                throw error(ProjectErrorCode.MEDIA_FILE_NOT_READY, "앨범 이미지 파일의 업로드 처리가 완료되지 않았습니다.");
            }
            project.updateMetadata(title.trim(), image.getId());
            return null;
        });
    }

    private ProjectApplicationException error(ProjectErrorCode code, String message) {
        return new ProjectApplicationException(code, message);
    }
}
