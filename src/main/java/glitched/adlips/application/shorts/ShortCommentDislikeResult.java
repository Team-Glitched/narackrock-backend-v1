package glitched.adlips.application.shorts;

public record ShortCommentDislikeResult(Long commentId, boolean isLiked, boolean isDisliked, int likeCount, int dislikeCount) {
    public ShortCommentDislikeResult(Long commentId, boolean isDisliked, int dislikeCount) {
        this(commentId, false, isDisliked, 0, dislikeCount);
    }
}
