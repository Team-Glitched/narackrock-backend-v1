package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.shorts.ShortCommentResult;

public record ShortCommentResponse(Long shortId, Long commentId, Long parentCommentId) {
    public static ShortCommentResponse from(ShortCommentResult result) {
        return new ShortCommentResponse(result.shortId(), result.commentId(), result.parentCommentId());
    }
}
