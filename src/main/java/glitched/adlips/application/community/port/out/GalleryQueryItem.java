package glitched.adlips.application.community.port.out;

import java.time.LocalDateTime;

public record GalleryQueryItem(
        Long galleryId,
        String name,
        String description,
        long subscriberCount,
        LocalDateTime createdAt,
        Long imageFileId, Long bannerImageFileId, String imageUrl, String bannerImageUrl
) {
    public GalleryQueryItem(Long galleryId, String name, String description, long subscriberCount, LocalDateTime createdAt) {
        this(galleryId, name, description, subscriberCount, createdAt, null, null, null, null);
    }
}
