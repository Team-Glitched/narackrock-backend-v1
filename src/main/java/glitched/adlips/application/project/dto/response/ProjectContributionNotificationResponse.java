package glitched.adlips.application.project.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectContributionNotificationResponse(List<Item> items) {
    public record Item(
            Long contributionId,
            Long projectId,
            String projectTitle,
            Long userId,
            String nickname,
            String description,
            int itemCount,
            LocalDateTime createdAt
    ) {
    }
}
