package glitched.adlips.application.community;

public record PostCommentReactionResult(
        long commentId, boolean liked, boolean disliked, int likeCount, int dislikeCount) {}
