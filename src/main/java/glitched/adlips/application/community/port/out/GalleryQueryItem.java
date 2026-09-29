package glitched.adlips.application.community.port.out;

import java.time.LocalDateTime;

public record GalleryQueryItem(
        Long galleryId,
        String name,
        String description,
        long subscriberCount,
        LocalDateTime createdAt
) {
}
