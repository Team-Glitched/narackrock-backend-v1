package glitched.adlips.application.community;

public record PostCommentResult(Long postId, Long commentId, Long parentCommentId) {

    public boolean isReply() {
        return parentCommentId != null;
    }
}
