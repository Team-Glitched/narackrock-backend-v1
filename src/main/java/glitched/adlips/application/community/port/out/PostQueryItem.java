package glitched.adlips.application.community.port.out;

import java.time.LocalDateTime;

public record PostQueryItem(
        Long postId,
        Long galleryId,
        String galleryName,
        Long userId,
        String nickname,
        String profileImageUrl,
        String title,
        String content,
        int viewCount,
        int likeCount,
        int dislikeCount,
        int commentCount,
        boolean liked,
        boolean disliked,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public PostQueryItem(
            Long postId,
            Long galleryId,
            Long userId,
            String nickname,
            String profileImageUrl,
            String title,
            String content,
            int viewCount,
            int likeCount,
            int dislikeCount,
            int commentCount,
            LocalDateTime createdAt
    ) {
        this(postId, galleryId, null, userId, nickname, profileImageUrl, title, content,
                viewCount, likeCount, dislikeCount, commentCount, false, false, createdAt, null);
    }
}
