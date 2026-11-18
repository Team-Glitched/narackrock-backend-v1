package glitched.adlips.application.project.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.port.out.EditorDraftRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectClipRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectContributionItemRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ContributionApprovalStatus;
import glitched.adlips.domain.project.ContributionTargetType;
import glitched.adlips.domain.project.ProjectStatus;

public class EmptyTrackDeleteUseCase {
    private final ProjectRepositoryPort projects;
    private final ProjectTrackRepositoryPort tracks;
    private final ProjectClipRepositoryPort clips;
    private final EditorDraftRepositoryPort drafts;
    private final ProjectContributionItemRepositoryPort contributionItems;
    private final TransactionRunner transactions;

    public EmptyTrackDeleteUseCase(ProjectRepositoryPort projects, ProjectTrackRepositoryPort tracks,
                                   ProjectClipRepositoryPort clips, EditorDraftRepositoryPort drafts,
                                   ProjectContributionItemRepositoryPort contributionItems,
                                   TransactionRunner transactions) {
        this.projects = projects;
        this.tracks = tracks;
        this.clips = clips;
        this.drafts = drafts;
        this.contributionItems = contributionItems;
        this.transactions = transactions;
    }

    public void execute(long projectId, long trackId, long userId) {
        transactions.required(() -> {
            deleteIfEmpty(projectId, trackId, userId);
            return null;
        });
    }

    private void deleteIfEmpty(long projectId, long trackId, long userId) {
        // Clip creation and draft writes take this same lock before reading the track.
        var project = projects.findByIdAndDeletedAtIsNullForUpdate(projectId).orElse(null);
        if (project == null || project.getStatus() == ProjectStatus.COMPLETED) return;

        var track = tracks.findTrackByIdAndIsDeletedFalse(trackId).orElse(null);
        if (track == null || !track.belongsTo(project)) return;
        if (!track.getOwner().getId().equals(userId)) {
            throw new ProjectApplicationException(ProjectErrorCode.TRACK_DELETE_DENIED, "트랙을 삭제할 권한이 없습니다.");
        }
        if (track.getApprovalStatus() != ApprovalStatus.DRAFT && track.getApprovalStatus() != ApprovalStatus.APPROVED) return;
        if (track.getMediaFileId() != null && track.getMediaFileId() > 0) return;
        if (!clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(trackId).isEmpty()) return;
        if (drafts.findActiveByProjectAndOwner(projectId, userId).stream().anyMatch(d -> d.trackId() == trackId)) return;
        if (contributionItems.existsByTargetTypeAndTargetIdAndContributionApprovalStatus(
                ContributionTargetType.TRACK, trackId, ContributionApprovalStatus.PENDING)) return;
        track.delete();
    }
}
