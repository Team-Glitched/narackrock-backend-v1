package glitched.adlips.application.project.usecase;

import glitched.adlips.application.notification.NotificationEvents;
import static glitched.adlips.domain.notification.NotificationType.*;

import glitched.adlips.application.project.port.out.ProjectClipRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectContributionRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectExportRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.ProjectContributionReviewRequest;
import glitched.adlips.application.project.dto.response.ProjectContributionReviewResponse;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.dto.response.ProjectVersionResponse;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.common.ContentLimits;
import glitched.adlips.domain.project.ContributionApprovalStatus;
import glitched.adlips.domain.project.ContributionChangeType;
import glitched.adlips.domain.project.ContributionTargetType;
import glitched.adlips.domain.project.ContributionVersionConflictException;
import glitched.adlips.domain.project.ProjectContribution;
import glitched.adlips.domain.project.ProjectContributionItem;
import glitched.adlips.domain.project.ProjectExport;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.project.ProjectStatus;
import glitched.adlips.domain.project.Project;
import java.time.LocalDateTime;
import java.util.Map;
public class ProjectContributionReviewUseCase {
    private final NotificationEvents notifications;
    private final ProjectContributionRepositoryPort contributions;
    private final ProjectMemberRepositoryPort members;
    private final ProjectTrackRepositoryPort tracks;
    private final ProjectClipRepositoryPort clips;
    private final UserRepositoryPort users;
    private final ProjectExportRepositoryPort exports;
    private final TransactionRunner transactionRunner;

    public ProjectContributionReviewUseCase(
            ProjectContributionRepositoryPort contributions,
            ProjectMemberRepositoryPort members,
            ProjectTrackRepositoryPort tracks,
            ProjectClipRepositoryPort clips,
            UserRepositoryPort users,
            ProjectExportRepositoryPort exports) {
        this(contributions, members, tracks, clips, users, exports, TransactionRunner.direct());
    }

    public ProjectContributionReviewUseCase(
            ProjectContributionRepositoryPort contributions,
            ProjectMemberRepositoryPort members,
            ProjectTrackRepositoryPort tracks,
            ProjectClipRepositoryPort clips,
            UserRepositoryPort users,
            ProjectExportRepositoryPort exports,
            TransactionRunner transactionRunner) {
        this(contributions, members, tracks, clips, users, exports, transactionRunner, NotificationEvents.none());
    }

    public ProjectContributionReviewUseCase(ProjectContributionRepositoryPort contributions,
            ProjectMemberRepositoryPort members,
            ProjectTrackRepositoryPort tracks,
            ProjectClipRepositoryPort clips,
            UserRepositoryPort users,
            ProjectExportRepositoryPort exports,
            TransactionRunner transactionRunner, NotificationEvents notifications) {
        this.contributions = contributions;
        this.members = members;
        this.tracks = tracks;
        this.clips = clips;
        this.users = users;
        this.exports = exports;
        this.transactionRunner = transactionRunner;
    
        this.notifications = notifications;
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
        if (ContentLimits.exceeds(reviewComment, ContentLimits.MODERATION_REASON_MAX_LENGTH)) {
            throw error(ProjectErrorCode.VALIDATION_ERROR,
                    "검토 의견은 " + ContentLimits.MODERATION_REASON_MAX_LENGTH + "자 이내로 입력해 주세요.");
        }
        if (request.approvalStatus() == ContributionApprovalStatus.APPROVED) {
            approve(contribution, reviewer, reviewComment, reviewedAt);
            contribution.getItems().forEach(this::applyApprovedItem);
            queuePublishedProjectRefresh(project);
        } else {
            contribution.reject(reviewer, reviewComment, reviewedAt);
            contribution.getItems().forEach(this::applyRejectedItem);
        }

        notifications.record(request.approvalStatus()==ContributionApprovalStatus.APPROVED?CONTRIBUTION_ACCEPTED:CONTRIBUTION_REJECTED,request.userId(),contribution.getId());
        return new ProjectContributionReviewResponse(
                request.projectId(), request.contributionId(), contribution.getApprovalStatus(),
                previousVersion, version(project.getMajorVersion(), project.getMinorVersion()),
                reviewer.getId(), contribution.getReviewedAt(), contribution.getReviewComment());
    }

    private void queuePublishedProjectRefresh(Project project) {
        if (project.isPublic() && project.getStatus() == ProjectStatus.IN_PROGRESS) {
            exports.save(new ProjectExport(project, project.getOwner()));
        }
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
            var track = tracks.findTrackByIdAndIsDeletedFalse(item.getTargetId())
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
        var clip = clips.findClipByIdAndIsDeletedFalse(item.getTargetId())
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
            tracks.findTrackByIdAndIsDeletedFalse(item.getTargetId()).orElseThrow(this::invalidItem).reject();
        } else {
            clips.findClipByIdAndIsDeletedFalse(item.getTargetId()).orElseThrow(this::invalidItem).reject();
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
