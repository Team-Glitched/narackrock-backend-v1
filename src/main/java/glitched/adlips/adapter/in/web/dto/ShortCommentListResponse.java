package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.shorts.ShortCommentListResult;
import glitched.adlips.application.shorts.port.out.ShortCommentQueryItem;
import java.time.LocalDateTime;
import java.util.List;

public record ShortCommentListResponse(Long shortId, List<CommentItem> comments) {

    public record CommentItem(
            Long commentId,
            Long parentCommentId,
            String content,
            Writer writer,
            int likeCount,
            int replyCount,
            boolean isLiked,
            LocalDateTime createdAt
    ) {
    }

    public record Writer(Long userId, String nickname, String profileImageUrl) {
    }

    public static ShortCommentListResponse from(ShortCommentListResult result) {
        List<CommentItem> items = result.comments().stream()
                .map(ShortCommentListResponse::toCommentItem)
                .toList();
        return new ShortCommentListResponse(result.shortId(), items);
    }

    private static CommentItem toCommentItem(ShortCommentQueryItem item) {
        return new CommentItem(
                item.commentId(),
                item.parentCommentId(),
                item.content(),
                new Writer(item.writerId(), item.nickname(), item.profileImageUrl()),
                item.likeCount(),
                item.replyCount(),
                item.liked(),
                item.createdAt());
    }
}
