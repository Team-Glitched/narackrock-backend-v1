package glitched.adlips.application.project.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.ProjectExportGetRequest;
import glitched.adlips.application.project.dto.response.ProjectExportGetResponse;
import glitched.adlips.application.project.dto.response.ProjectVersionResponse;
import glitched.adlips.application.project.port.out.ProjectExportRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;

public class ProjectExportGetUseCase {

    private final ProjectRepositoryPort projects;
    private final ProjectMemberRepositoryPort members;
    private final ProjectExportRepositoryPort exports;
    private final MediaFileRepositoryPort mediaFiles;
    private final TransactionRunner transactionRunner;

    public ProjectExportGetUseCase(
            ProjectRepositoryPort projects,
            ProjectMemberRepositoryPort members,
            ProjectExportRepositoryPort exports,
            MediaFileRepositoryPort mediaFiles
    ) {
        this(projects, members, exports, mediaFiles, TransactionRunner.direct());
    }

    public ProjectExportGetUseCase(
            ProjectRepositoryPort projects,
            ProjectMemberRepositoryPort members,
            ProjectExportRepositoryPort exports,
            MediaFileRepositoryPort mediaFiles,
            TransactionRunner transactionRunner
    ) {
        this.projects = projects;
        this.members = members;
        this.exports = exports;
        this.mediaFiles = mediaFiles;
        this.transactionRunner = transactionRunner;
    }

    public ProjectExportGetResponse execute(ProjectExportGetRequest request) {
        return transactionRunner.readOnly(() -> executeInternal(request));
    }

    private ProjectExportGetResponse executeInternal(ProjectExportGetRequest request) {
        var project = projects.findByIdAndDeletedAtIsNull(request.projectId())
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_NOT_FOUND,
                        "존재하지 않거나 삭제된 음악 프로젝트입니다."));
        members.findByProjectIdAndUserId(request.projectId(), request.userId())
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_ACCESS_DENIED,
                        "해당 프로젝트의 Export를 조회할 권한이 없습니다."));
        var export = exports.findExportByIdAndProjectId(request.exportId(), request.projectId())
                .orElseThrow(() -> error(ProjectErrorCode.EXPORT_NOT_FOUND,
                        "해당 프로젝트의 Export를 찾을 수 없습니다."));

        Long mixedAudioFileId = export.getMediaFileId();
        Long albumImageFileId = project.getAlbumImageFileId();
        var version = new ProjectVersionResponse(
                export.getProjectMajorVersion(),
                export.getProjectMinorVersion(),
                "v" + export.getProjectMajorVersion() + "." + export.getProjectMinorVersion());

        return new ProjectExportGetResponse(
                project.getId(),
                export.getId(),
                version,
                mixedAudioFileId,
                url(mixedAudioFileId),
                export.getLayerArchiveFileId(),
                url(export.getLayerArchiveFileId()),
                export.getShortId(),
                null,
                null,
                albumImageFileId,
                url(albumImageFileId),
                export.getDurationMs(),
                export.getStatus(),
                export.getErrorMessage(),
                export.getCreatedAt(),
                export.getCompletedAt());
    }

    private String url(Long mediaFileId) {
        return mediaFileId == null
                ? null
                : mediaFiles.findById(mediaFileId).map(it -> it.getFileUrl()).orElse(null);
    }

    private ProjectApplicationException error(ProjectErrorCode code, String message) {
        return new ProjectApplicationException(code, message);
    }
}
