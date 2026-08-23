package glitched.adlips.application.community.port.out;

import java.time.LocalDateTime;

public record PostQueryItem(
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
}
