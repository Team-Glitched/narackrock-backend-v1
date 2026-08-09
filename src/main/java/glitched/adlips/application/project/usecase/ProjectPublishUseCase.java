package glitched.adlips.application.project.usecase;

import glitched.adlips.adapter.out.persistence.project.*;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.ProjectPublishRequest;
import glitched.adlips.application.project.dto.response.ProjectPublishResponse;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.dto.response.ProjectVersionResponse;
import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ProjectExport;
import glitched.adlips.domain.project.ProjectMemberRole;
import java.util.List;
public class ProjectPublishUseCase {
    private final ProjectJpaRepository projects;
    private final ProjectMemberJpaRepository members;
    private final ProjectTrackJpaRepository tracks;
    private final ProjectClipJpaRepository clips;
    private final ProjectExportJpaRepository exports;
    private final TransactionRunner transactionRunner;

    public ProjectPublishUseCase(ProjectJpaRepository projects, ProjectMemberJpaRepository members,
                                 ProjectTrackJpaRepository tracks, ProjectClipJpaRepository clips,
                                 ProjectExportJpaRepository exports) {
        this(projects, members, tracks, clips, exports, TransactionRunner.direct());
    }

    public ProjectPublishUseCase(ProjectJpaRepository projects, ProjectMemberJpaRepository members,
                                 ProjectTrackJpaRepository tracks, ProjectClipJpaRepository clips,
                                 ProjectExportJpaRepository exports, TransactionRunner transactionRunner) {
        this.projects = projects; this.members = members; this.tracks = tracks;
        this.clips = clips; this.exports = exports;
        this.transactionRunner = transactionRunner;
    }

    public ProjectPublishResponse execute(ProjectPublishRequest request) {
        return transactionRunner.required(() -> executeInternal(request));
    }

    private ProjectPublishResponse executeInternal(ProjectPublishRequest request) {
        var project = projects.findByIdAndDeletedAtIsNull(request.projectId())
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_NOT_FOUND, "존재하지 않는 프로젝트입니다."));
        members.findByProjectIdAndUserId(request.projectId(), request.userId())
                .filter(member -> member.getRole() == ProjectMemberRole.OWNER)
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "곡을 공개할 권한이 없습니다."));
        List<Long> approvedTrackIds = tracks.findByProjectIdAndApprovalStatusAndIsDeletedFalse(
                request.projectId(), ApprovalStatus.APPROVED).stream().map(it -> it.getId()).toList();
        if (approvedTrackIds.isEmpty() || clips.countByTrackIdInAndApprovalStatusAndIsDeletedFalse(
                approvedTrackIds, ApprovalStatus.APPROVED) == 0) {
            throw error(ProjectErrorCode.EXPORT_TARGET_NOT_FOUND, "공개할 승인된 클립이 없습니다.");
        }
        project.startPublishing();
        ProjectExport export = exports.save(new ProjectExport(project, project.getOwner()));
        return new ProjectPublishResponse(project.getId(), export.getId(),
                new ProjectVersionResponse(project.getMajorVersion(), project.getMinorVersion(), project.getDisplayVersion()),
                null, null, export.getStatus());
    }
    private ProjectApplicationException error(ProjectErrorCode code, String message) {
        return new ProjectApplicationException(code, message);
    }
}
