package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.shorts.ShortCommentDislikeResult;

public record ShortCommentDislikeResponse(Long commentId, boolean isDisliked, int dislikeCount) {
    public static ShortCommentDislikeResponse from(ShortCommentDislikeResult result) {
        return new ShortCommentDislikeResponse(result.commentId(), result.isDisliked(), result.dislikeCount());
    }
}
