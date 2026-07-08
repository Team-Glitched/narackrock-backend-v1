package glitched.adlips.application.shorts;

public class ShortCommentApplicationException extends RuntimeException {

    private final ShortCommentErrorCode errorCode;

    public ShortCommentApplicationException(ShortCommentErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ShortCommentErrorCode getErrorCode() {
        return errorCode;
    }
}
