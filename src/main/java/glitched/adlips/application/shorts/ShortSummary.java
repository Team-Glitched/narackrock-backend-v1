package glitched.adlips.application.shorts;

import java.time.LocalDateTime;
import java.util.List;

public record ShortSummary(
        Long shortId,
        Long projectId,
        String title,
        String mediaUrl,
        String albumImageUrl,
        String completionStatus,
        int viewCount,
        int likeCount,
        int dislikeCount,
        long commentCount,
        int contributionCount,
        boolean isLiked,
        boolean isDisliked,
        boolean isBookmarked,
        AuthorSummary author,
        List<ParticipantSummary> participants,
        LocalDateTime createdAt
) {}
