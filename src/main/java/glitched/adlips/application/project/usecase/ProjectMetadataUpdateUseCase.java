package glitched.adlips.application.project.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.PublishedShortPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.media.MediaFileStatus;
import glitched.adlips.domain.media.MediaFileType;
import glitched.adlips.domain.project.ProjectMemberRole;

public class ProjectMetadataUpdateUseCase {
    private final ProjectRepositoryPort projects;
    private final ProjectMemberRepositoryPort members;
    private final MediaFileRepositoryPort mediaFiles;
    private final TransactionRunner transactions;
    private final PublishedShortPort publishedShorts;

    public ProjectMetadataUpdateUseCase(ProjectRepositoryPort projects, ProjectMemberRepositoryPort members,
                                        MediaFileRepositoryPort mediaFiles, PublishedShortPort publishedShorts) {
        this(projects, members, mediaFiles, publishedShorts, TransactionRunner.direct());
    }

    public ProjectMetadataUpdateUseCase(ProjectRepositoryPort projects, ProjectMemberRepositoryPort members,
                                        MediaFileRepositoryPort mediaFiles, PublishedShortPort publishedShorts, TransactionRunner transactions) {
        this.projects = projects;
        this.members = members;
        this.mediaFiles = mediaFiles;
        this.transactions = transactions;
        this.publishedShorts = publishedShorts;
    }

    public void execute(Long projectId, Long userId, String title, Long albumImageFileId) {
        transactions.required(() -> {
            if (albumImageFileId != null && albumImageFileId < 0) {
                throw error(ProjectErrorCode.VALIDATION_ERROR, "앨범 정보를 확인해 주세요.");
            }
            String trimmedTitle = title == null ? "" : title.strip();
            if (trimmedTitle.isEmpty()
                    || trimmedTitle.codePointCount(0, trimmedTitle.length()) > ProjectCreateUseCase.MAX_TITLE_LENGTH) {
                throw error(ProjectErrorCode.VALIDATION_ERROR, "곡 이름은 1~40자 이내로 입력해 주세요.");
            }
            var project = projects.findByIdAndDeletedAtIsNullForUpdate(projectId)
                    .orElseThrow(() -> error(ProjectErrorCode.PROJECT_NOT_FOUND, "존재하지 않는 프로젝트입니다."));
            members.findByProjectIdAndUserId(projectId, userId)
                    .filter(member -> member.getRole() == ProjectMemberRole.OWNER)
                    .orElseThrow(() -> error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "곡 정보를 변경할 권한이 없습니다."));
            if (!project.getOwner().getId().equals(userId)) {
                throw error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "곡 정보를 변경할 권한이 없습니다.");
            }
            Long imageId = albumImageFileId == null ? project.getAlbumImageFileId() : albumImageFileId;
            if (imageId > 0) {
                MediaFile image = mediaFiles.findById(imageId)
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
            }
            project.updateMetadata(trimmedTitle, imageId);
            publishedShorts.updateMetadata(project);
            return null;
        });
    }

    private ProjectApplicationException error(ProjectErrorCode code, String message) {
        return new ProjectApplicationException(code, message);
    }
}
