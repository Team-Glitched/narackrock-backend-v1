package glitched.adlips.application.community;

public class PostApplicationException extends RuntimeException {

    private final PostErrorCode errorCode;

    public PostApplicationException(PostErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public PostErrorCode getErrorCode() {
        return errorCode;
    }
}
