package glitched.adlips.application.community.port.out;

import glitched.adlips.domain.community.GalleryModerationActionType;
import java.time.LocalDateTime;

public record GalleryModerationLogItem(
        Long logId,
        GalleryModerationActionType actionType,
        Long actorId,
        Long targetUserId,
        Long targetId,
        String reason,
        LocalDateTime createdAt
) {
}
