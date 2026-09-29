package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.community.PostReactionResult;

public record PostReactionResponse(
        long postId,
        boolean isLiked,
        boolean isDisliked,
        int likeCount,
        int dislikeCount
) {

    public static PostReactionResponse from(PostReactionResult result) {
        return new PostReactionResponse(
                result.postId(), result.liked(), result.disliked(), result.likeCount(), result.dislikeCount());
    }
}
