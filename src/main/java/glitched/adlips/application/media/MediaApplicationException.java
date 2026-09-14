package glitched.adlips.application.media;

public class MediaApplicationException extends RuntimeException {
    private final MediaErrorCode errorCode;

    public MediaApplicationException(MediaErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public MediaErrorCode getErrorCode() {
        return errorCode;
    }
}
