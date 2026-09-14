package glitched.adlips.application.project.dto.response;

import glitched.adlips.domain.project.ContributionApprovalStatus;
import glitched.adlips.domain.project.ContributionChangeType;
import glitched.adlips.domain.project.ContributionItemStatus;
import glitched.adlips.domain.project.ContributionTargetType;
import java.time.LocalDateTime;
import java.util.List;

public record ProjectContributionCreateResponse(
        Long projectId,
        Long contributionId,
        ProjectVersionResponse baseProjectVersion,
        ContributionApprovalStatus approvalStatus,
        String description,
        List<Item> items,
        LocalDateTime createdAt
) {
    public record Item(
            ContributionChangeType changeType,
            ContributionTargetType targetType,
            Long targetId,
            ContributionItemStatus itemStatus
    ) {
    }
}
