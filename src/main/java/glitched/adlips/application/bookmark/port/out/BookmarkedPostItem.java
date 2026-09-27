package glitched.adlips.application.bookmark.port.out;

import java.time.LocalDateTime;

public record BookmarkedPostItem(
        Long postId,
        Long galleryId,
        String galleryName,
        Long writerId,
        String nickname,
        String profileImageUrl,
        String title,
        String content,
        String imageUrl,
        int viewCount,
        int likeCount,
        int commentCount,
        LocalDateTime createdAt,
        LocalDateTime bookmarkedAt
) {}
