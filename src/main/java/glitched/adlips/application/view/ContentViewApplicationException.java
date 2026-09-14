package glitched.adlips.application.view;

public class ContentViewApplicationException extends RuntimeException {

    private final ContentViewErrorCode errorCode;

    public ContentViewApplicationException(ContentViewErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ContentViewErrorCode getErrorCode() {
        return errorCode;
    }
}
