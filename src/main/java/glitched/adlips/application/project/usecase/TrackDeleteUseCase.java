package glitched.adlips.application.project.usecase;

import glitched.adlips.adapter.out.persistence.project.ProjectContributionItemJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectTrackJpaRepository;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.TrackDeleteRequest;
import glitched.adlips.application.project.dto.response.ProjectVersionResponse;
import glitched.adlips.application.project.dto.response.TrackDeleteResponse;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.project.ContributionApprovalStatus;
import glitched.adlips.domain.project.ContributionTargetType;
public class TrackDeleteUseCase {
    private final ProjectTrackJpaRepository tracks;
    private final ProjectContributionItemJpaRepository contributionItems;
    private final TransactionRunner transactionRunner;

    public TrackDeleteUseCase(ProjectTrackJpaRepository tracks,
                              ProjectContributionItemJpaRepository contributionItems) {
        this(tracks, contributionItems, TransactionRunner.direct());
    }

    public TrackDeleteUseCase(ProjectTrackJpaRepository tracks,
                              ProjectContributionItemJpaRepository contributionItems,
                              TransactionRunner transactionRunner) {
        this.tracks = tracks; this.contributionItems = contributionItems;
        this.transactionRunner = transactionRunner;
    }

    public TrackDeleteResponse execute(TrackDeleteRequest request) {
        return transactionRunner.required(() -> executeInternal(request));
    }

    private TrackDeleteResponse executeInternal(TrackDeleteRequest request) {
        var track = tracks.findByIdAndIsDeletedFalse(request.trackId())
                .orElseThrow(() -> error(ProjectErrorCode.TRACK_NOT_FOUND, "존재하지 않는 트랙입니다."));
        Long projectOwnerId = track.getProject().getOwner().getId();
        if (!request.userId().equals(track.getOwner().getId()) && !request.userId().equals(projectOwnerId)) {
            throw error(ProjectErrorCode.TRACK_DELETE_DENIED, "트랙을 삭제할 권한이 없습니다.");
        }
        if (contributionItems.existsByTargetTypeAndTargetIdAndContributionApprovalStatus(
                ContributionTargetType.TRACK, track.getId(), ContributionApprovalStatus.PENDING)) {
            throw error(ProjectErrorCode.TRACK_HAS_PENDING_CONTRIBUTION, "처리 중인 기여가 있는 트랙은 삭제할 수 없습니다.");
        }
        track.delete();
        var project = track.getProject();
        return new TrackDeleteResponse(track.getId(), new ProjectVersionResponse(
                project.getMajorVersion(), project.getMinorVersion(), project.getDisplayVersion()));
    }
    private ProjectApplicationException error(ProjectErrorCode code, String message) {
        return new ProjectApplicationException(code, message);
    }
}
