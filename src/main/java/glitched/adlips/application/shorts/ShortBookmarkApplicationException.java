package glitched.adlips.application.shorts;

public class ShortBookmarkApplicationException extends RuntimeException {

    private final ShortBookmarkErrorCode errorCode;

    public ShortBookmarkApplicationException(ShortBookmarkErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ShortBookmarkErrorCode getErrorCode() {
        return errorCode;
    }
}
