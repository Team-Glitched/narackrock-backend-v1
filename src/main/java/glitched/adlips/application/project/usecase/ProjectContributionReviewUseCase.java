package glitched.adlips.application.project.usecase;

import glitched.adlips.adapter.out.persistence.project.ProjectClipJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectContributionJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectMemberJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectTrackJpaRepository;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.ProjectContributionReviewRequest;
import glitched.adlips.application.project.dto.response.ProjectContributionReviewResponse;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.dto.response.ProjectVersionResponse;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.project.ContributionApprovalStatus;
import glitched.adlips.domain.project.ContributionChangeType;
import glitched.adlips.domain.project.ContributionTargetType;
import glitched.adlips.domain.project.ContributionVersionConflictException;
import glitched.adlips.domain.project.ProjectContribution;
import glitched.adlips.domain.project.ProjectContributionItem;
import glitched.adlips.domain.project.ProjectMemberRole;
import java.time.LocalDateTime;
import java.util.Map;
public class ProjectContributionReviewUseCase {
    private final ProjectContributionJpaRepository contributions;
    private final ProjectMemberJpaRepository members;
    private final ProjectTrackJpaRepository tracks;
    private final ProjectClipJpaRepository clips;
    private final UserRepositoryPort users;
    private final TransactionRunner transactionRunner;

    public ProjectContributionReviewUseCase(
            ProjectContributionJpaRepository contributions,
            ProjectMemberJpaRepository members,
            ProjectTrackJpaRepository tracks,
            ProjectClipJpaRepository clips,
            UserRepositoryPort users) {
        this(contributions, members, tracks, clips, users, TransactionRunner.direct());
    }

    public ProjectContributionReviewUseCase(
            ProjectContributionJpaRepository contributions,
            ProjectMemberJpaRepository members,
            ProjectTrackJpaRepository tracks,
            ProjectClipJpaRepository clips,
            UserRepositoryPort users,
            TransactionRunner transactionRunner) {
        this.contributions = contributions;
        this.members = members;
        this.tracks = tracks;
        this.clips = clips;
        this.users = users;
        this.transactionRunner = transactionRunner;
    }

    public ProjectContributionReviewResponse execute(ProjectContributionReviewRequest request) {
        return transactionRunner.required(() -> executeInternal(request));
    }

    private ProjectContributionReviewResponse executeInternal(ProjectContributionReviewRequest request) {
        validateRequest(request);
        ProjectContribution contribution = contributions
                .findByIdAndProjectId(request.contributionId(), request.projectId())
                .orElseThrow(() -> error(ProjectErrorCode.CONTRIBUTION_NOT_FOUND,
                        "존재하지 않는 기여 요청입니다."));
        members.findByProjectIdAndUserId(request.projectId(), request.userId())
                .filter(member -> member.getRole() == ProjectMemberRole.OWNER)
                .orElseThrow(() -> error(ProjectErrorCode.CONTRIBUTION_REVIEW_DENIED,
                        "해당 기여 요청을 승인하거나 거부할 권한이 없습니다."));
        var reviewer = users.findById(request.userId()).filter(it -> it.isActive())
                .orElseThrow(() -> error(ProjectErrorCode.CONTRIBUTION_REVIEW_DENIED,
                        "해당 기여 요청을 승인하거나 거부할 권한이 없습니다."));
        if (contribution.getApprovalStatus() != ContributionApprovalStatus.PENDING) {
            throw error(ProjectErrorCode.CONTRIBUTION_ALREADY_REVIEWED,
                    "이미 승인 또는 거부 처리된 기여 요청입니다.");
        }

        var project = contribution.getProject();
        ProjectVersionResponse previousVersion = version(project.getMajorVersion(), project.getMinorVersion());
        LocalDateTime reviewedAt = LocalDateTime.now();
        String reviewComment = normalize(request.reviewComment());
        if (request.approvalStatus() == ContributionApprovalStatus.APPROVED) {
            approve(contribution, reviewer, reviewComment, reviewedAt);
            contribution.getItems().forEach(this::applyApprovedItem);
        } else {
            contribution.reject(reviewer, reviewComment, reviewedAt);
            contribution.getItems().forEach(this::applyRejectedItem);
        }

        return new ProjectContributionReviewResponse(
                request.projectId(), request.contributionId(), contribution.getApprovalStatus(),
                previousVersion, version(project.getMajorVersion(), project.getMinorVersion()),
                reviewer.getId(), contribution.getReviewedAt(), contribution.getReviewComment());
    }

    private void approve(ProjectContribution contribution, glitched.adlips.domain.user.User reviewer,
                         String reviewComment, LocalDateTime reviewedAt) {
        try {
            contribution.approve(reviewer, reviewComment, reviewedAt);
        } catch (ContributionVersionConflictException exception) {
            throw new ProjectApplicationException(
                    ProjectErrorCode.CONTRIBUTION_VERSION_CONFLICT,
                    "기여 요청 이후 프로젝트가 변경되었습니다.",
                    Map.of(
                            "baseProjectVersion", version(exception.getBaseMajorVersion(),
                                    exception.getBaseMinorVersion()),
                            "currentProjectVersion", version(exception.getCurrentMajorVersion(),
                                    exception.getCurrentMinorVersion())));
        }
    }

    private void applyApprovedItem(ProjectContributionItem item) {
        if (item.getTargetType() == ContributionTargetType.TRACK) {
            var track = tracks.findByIdAndIsDeletedFalse(item.getTargetId())
                    .orElseThrow(this::invalidItem);
            switch (item.getChangeType()) {
                case ADD -> {
                    track.approve();
                    track.invalidateRenderedMedia();
                }
                case UPDATE -> track.invalidateRenderedMedia();
                case DELETE -> track.deleteForContribution();
            }
            return;
        }
        var clip = clips.findByIdAndIsDeletedFalse(item.getTargetId())
                .orElseThrow(this::invalidItem);
        switch (item.getChangeType()) {
            case ADD -> {
                clip.approve();
                clip.getTrack().invalidateRenderedMedia();
            }
            case UPDATE -> clip.getTrack().invalidateRenderedMedia();
            case DELETE -> clip.deleteForContribution();
        }
    }

    private void applyRejectedItem(ProjectContributionItem item) {
        if (item.getChangeType() != ContributionChangeType.ADD) {
            return;
        }
        if (item.getTargetType() == ContributionTargetType.TRACK) {
            tracks.findByIdAndIsDeletedFalse(item.getTargetId()).orElseThrow(this::invalidItem).reject();
        } else {
            clips.findByIdAndIsDeletedFalse(item.getTargetId()).orElseThrow(this::invalidItem).reject();
        }
    }

    private void validateRequest(ProjectContributionReviewRequest request) {
        if (request == null || request.projectId() == null || request.contributionId() == null
                || request.userId() == null
                || (request.approvalStatus() != ContributionApprovalStatus.APPROVED
                && request.approvalStatus() != ContributionApprovalStatus.REJECTED)) {
            throw error(ProjectErrorCode.VALIDATION_ERROR, "승인 또는 거부 상태를 확인해 주세요.");
        }
    }

    private ProjectVersionResponse version(int major, int minor) {
        return new ProjectVersionResponse(major, minor, "v" + major + "." + minor);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ProjectApplicationException invalidItem() {
        return error(ProjectErrorCode.INVALID_CONTRIBUTION_ITEM, "기여 항목 정보가 올바르지 않습니다.");
    }

    private ProjectApplicationException error(ProjectErrorCode code, String message) {
        return new ProjectApplicationException(code, message);
    }
}
