package glitched.adlips.application.community;

public record PostReactionResult(long postId, boolean liked, boolean disliked, int likeCount, int dislikeCount) {}
