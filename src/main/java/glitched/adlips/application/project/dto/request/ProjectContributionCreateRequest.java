package glitched.adlips.application.project.dto.request;

import glitched.adlips.domain.project.ContributionChangeType;
import glitched.adlips.domain.project.ContributionTargetType;
import java.util.List;

public record ProjectContributionCreateRequest(
        Long projectId,
        Long userId,
        String description,
        ProjectVersionRequest baseProjectVersion,
        List<Item> items
) {
    public record Item(
            ContributionChangeType changeType,
            ContributionTargetType targetType,
            Long targetId
    ) {
    }
}
