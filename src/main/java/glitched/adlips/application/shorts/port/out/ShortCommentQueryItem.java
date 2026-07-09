package glitched.adlips.application.shorts.port.out;

import java.time.LocalDateTime;

public record ShortCommentQueryItem(
        Long commentId,
        Long parentCommentId,
        String content,
        Long writerId,
        String nickname,
        String profileImageUrl,
        int likeCount,
        int replyCount,
        boolean liked,
        LocalDateTime createdAt
) {
}
