package glitched.adlips.application.project.dto.response;

import glitched.adlips.domain.project.ContributionApprovalStatus;
import java.time.LocalDateTime;

public record ProjectContributionReviewResponse(
        Long projectId,
        Long contributionId,
        ContributionApprovalStatus approvalStatus,
        ProjectVersionResponse previousProjectVersion,
        ProjectVersionResponse projectVersion,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        String reviewComment
) {
}
