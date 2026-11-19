package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.community.port.out.GalleryQueryItem;
import java.time.LocalDateTime;

public record GalleryListItemResponse(
        Long galleryId,
        String name,
        String description,
        long subscriberCount,
        LocalDateTime createdAt,
        Long imageFileId, Long bannerImageFileId, String imageUrl, String bannerImageUrl
) {

    public static GalleryListItemResponse from(GalleryQueryItem item) {
        return new GalleryListItemResponse(
                item.galleryId(),
                item.name(),
                item.description(),
                item.subscriberCount(),
                item.createdAt(), item.imageFileId(), item.bannerImageFileId(), item.imageUrl(), item.bannerImageUrl());
    }
}
