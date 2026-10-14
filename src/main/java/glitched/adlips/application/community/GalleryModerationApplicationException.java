package glitched.adlips.application.community;

public class GalleryModerationApplicationException extends RuntimeException {

    private final GalleryModerationErrorCode errorCode;

    public GalleryModerationApplicationException(GalleryModerationErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public GalleryModerationErrorCode getErrorCode() {
        return errorCode;
    }
}
