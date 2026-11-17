package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.community.port.out.GalleryManagerItem;
import java.time.LocalDateTime;

public record GalleryManagerResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        LocalDateTime assignedAt
) {

    public static GalleryManagerResponse from(GalleryManagerItem item) {
        return new GalleryManagerResponse(item.userId(), item.nickname(), item.profileImageUrl(), item.assignedAt());
    }
}
