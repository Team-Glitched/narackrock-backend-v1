package glitched.adlips.application.community;

public class PostCommentApplicationException extends RuntimeException {

    private final PostCommentErrorCode errorCode;

    public PostCommentApplicationException(PostCommentErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public PostCommentErrorCode getErrorCode() {
        return errorCode;
    }
}
