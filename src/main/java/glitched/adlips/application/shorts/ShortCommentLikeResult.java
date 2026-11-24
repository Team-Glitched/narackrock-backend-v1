package glitched.adlips.application.shorts;

public record ShortCommentLikeResult(Long commentId, boolean isLiked, boolean isDisliked, int likeCount, int dislikeCount) {
    public ShortCommentLikeResult(Long commentId, boolean isLiked, int likeCount) {
        this(commentId, isLiked, false, likeCount, 0);
    }
}
