package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.community.PostCommentListResult;
import java.time.LocalDateTime;
import java.util.List;

public record PostCommentListResponse(Long postId, Long parentCommentId, int page, int size,
        long totalCount, List<Comment> comments) {
    public record Writer(Long userId, String nickname, String profileImageUrl) {}
    public record Comment(Long commentId, Long parentCommentId, Writer writer, String content,
            long replyCount, int likeCount, int dislikeCount, boolean isLiked, boolean isDisliked,
            LocalDateTime createdAt) {}
    public static PostCommentListResponse from(PostCommentListResult result) {
        return new PostCommentListResponse(result.postId(), result.parentCommentId(), result.page(),
                result.size(), result.totalCount(), result.comments().stream().map(c -> new Comment(
                        c.commentId(), c.parentCommentId(), new Writer(c.userId(), c.nickname(), c.profileImageUrl()),
                        c.content(), c.replyCount(), c.likeCount(), c.dislikeCount(), c.liked(), c.disliked(),
                        c.createdAt())).toList());
    }
}
