package glitched.adlips.application.bookmark.port.out;

import java.time.LocalDateTime;

public record BookmarkedShortItem(
        Long bookmarkId,
        Long shortId,
        String title,
        String albumImageUrl,
        String status,
        Long authorId,
        String nickname,
        String profileImageUrl,
        int likeCount,
        long commentCount,
        LocalDateTime bookmarkedAt
) {}
