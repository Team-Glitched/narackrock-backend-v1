package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.community.PostCommentResult;

public record PostCommentCreateResponse(Long commentId) {
    public static PostCommentCreateResponse from(PostCommentResult result) {
        return new PostCommentCreateResponse(result.commentId());
    }
}
