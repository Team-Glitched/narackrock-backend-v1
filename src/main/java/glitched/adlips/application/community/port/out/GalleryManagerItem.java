package glitched.adlips.application.community.port.out;

import java.time.LocalDateTime;

public record GalleryManagerItem(
        Long userId,
        String nickname,
        String profileImageUrl,
        LocalDateTime assignedAt
) {
}
