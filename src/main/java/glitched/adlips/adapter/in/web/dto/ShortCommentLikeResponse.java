package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.shorts.ShortCommentLikeResult;

public record ShortCommentLikeResponse(Long commentId, boolean isLiked, int likeCount) {
    public static ShortCommentLikeResponse from(ShortCommentLikeResult result) {
        return new ShortCommentLikeResponse(result.commentId(), result.isLiked(), result.likeCount());
    }
}
