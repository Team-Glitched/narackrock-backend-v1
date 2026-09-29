package glitched.adlips.application.community.port.out;

import java.time.LocalDateTime;

public record PostCommentQueryItem(Long commentId, Long parentCommentId, Long userId,
        String nickname, String profileImageUrl, String content, long replyCount,
        int likeCount, int dislikeCount, boolean liked, boolean disliked, LocalDateTime createdAt) {}
