package glitched.adlips.application.community;

public class GalleryApplicationException extends RuntimeException {

    private final GalleryErrorCode errorCode;

    public GalleryApplicationException(GalleryErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public GalleryErrorCode getErrorCode() {
        return errorCode;
    }
}
