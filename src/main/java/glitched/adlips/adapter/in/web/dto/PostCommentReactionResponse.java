package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.community.PostCommentReactionResult;

public record PostCommentReactionResponse(
        long commentId,
        boolean isLiked,
        boolean isDisliked,
        int likeCount,
        int dislikeCount
) {

    public static PostCommentReactionResponse from(PostCommentReactionResult result) {
        return new PostCommentReactionResponse(
                result.commentId(), result.liked(), result.disliked(), result.likeCount(), result.dislikeCount());
    }
}
