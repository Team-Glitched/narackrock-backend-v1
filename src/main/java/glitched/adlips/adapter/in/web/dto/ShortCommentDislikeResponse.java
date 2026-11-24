package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.shorts.ShortCommentDislikeResult;

public record ShortCommentDislikeResponse(Long commentId, boolean isLiked, boolean isDisliked, int likeCount, int dislikeCount) {
    public ShortCommentDislikeResponse(Long commentId, boolean isDisliked, int dislikeCount) {
        this(commentId, false, isDisliked, 0, dislikeCount);
    }

    public static ShortCommentDislikeResponse from(ShortCommentDislikeResult result) {
        return new ShortCommentDislikeResponse(result.commentId(), result.isLiked(), result.isDisliked(),
                result.likeCount(), result.dislikeCount());
    }
}
