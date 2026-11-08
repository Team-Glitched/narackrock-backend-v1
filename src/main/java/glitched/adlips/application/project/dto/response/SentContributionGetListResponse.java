package glitched.adlips.application.project.dto.response;

import glitched.adlips.domain.project.ContributionApprovalStatus;
import java.time.LocalDateTime;
import java.util.List;

public record SentContributionGetListResponse(List<Item> items, ProjectContributionGetListResponse.Page page) {
    public record Item(Long contributionId, Long projectId, String projectTitle, String description,
                       ContributionApprovalStatus approvalStatus, LocalDateTime createdAt) {}
}
