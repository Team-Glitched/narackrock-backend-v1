package glitched.adlips.application.project.dto.response;

import glitched.adlips.domain.project.ContributionApprovalStatus;
import glitched.adlips.domain.project.ContributionChangeType;
import glitched.adlips.domain.project.ContributionItemStatus;
import glitched.adlips.domain.project.ContributionTargetType;
import java.time.LocalDateTime;
import java.util.List;

public record ProjectContributionGetListResponse(List<Contribution> items, Page page) {
    public record Contribution(
            Long contributionId,
            Long projectId,
            Long userId,
            String nickname,
            String profileImageUrl,
            String description,
            ProjectVersionResponse baseProjectVersion,
            ContributionApprovalStatus approvalStatus,
            Long reviewedBy,
            LocalDateTime reviewedAt,
            String reviewComment,
            LocalDateTime createdAt,
            List<Item> items
    ) {
    }

    public record Item(
            ContributionChangeType changeType,
            ContributionTargetType targetType,
            Long targetId,
            ContributionItemStatus itemStatus
    ) {
    }

    public record Page(Long nextCursor, boolean hasMore, int size) {
    }
}
