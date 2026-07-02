package glitched.adlips.application.shorts.port.out;

import glitched.adlips.domain.shorts.ShortStatus;
import java.time.LocalDateTime;

public record ShortsQueryItem(
        Long shortId,
        Long projectId,
        String title,
        String mediaUrl,
        String albumImageUrl,
        ShortStatus status,
        int viewCount,
        int likeCount,
        int dislikeCount,
        long commentCount,
        int contributionCount,
        boolean liked,
        boolean disliked,
        boolean bookmarked,
        Long authorId,
        String authorNickname,
        String authorProfileImageUrl,
        LocalDateTime createdAt
) {
}
