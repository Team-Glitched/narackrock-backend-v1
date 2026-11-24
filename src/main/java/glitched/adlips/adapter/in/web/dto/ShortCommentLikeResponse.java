package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.shorts.ShortCommentLikeResult;

public record ShortCommentLikeResponse(Long commentId, boolean isLiked, boolean isDisliked, int likeCount, int dislikeCount) {
    public ShortCommentLikeResponse(Long commentId, boolean isLiked, int likeCount) {
        this(commentId, isLiked, false, likeCount, 0);
    }

    public static ShortCommentLikeResponse from(ShortCommentLikeResult result) {
        return new ShortCommentLikeResponse(result.commentId(), result.isLiked(), result.isDisliked(),
                result.likeCount(), result.dislikeCount());
    }
}
