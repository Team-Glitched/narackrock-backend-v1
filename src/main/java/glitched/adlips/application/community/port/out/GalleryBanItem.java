package glitched.adlips.application.community.port.out;

import glitched.adlips.domain.community.GalleryBanType;
import java.time.LocalDateTime;

public record GalleryBanItem(
        Long banId,
        Long userId,
        Long actorId,
        GalleryBanType banType,
        String reason,
        LocalDateTime bannedUntil,
        LocalDateTime liftedAt,
        LocalDateTime createdAt,
        boolean active
) {
}
