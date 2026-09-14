package glitched.adlips.application.project.dto.request;

import glitched.adlips.domain.project.ContributionApprovalStatus;

public record ProjectContributionReviewRequest(
        Long projectId,
        Long contributionId,
        Long userId,
        ContributionApprovalStatus approvalStatus,
        String reviewComment
) {
}
