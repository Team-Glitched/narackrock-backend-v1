package glitched.adlips.application.shorts;

public class ShortPlaybackApplicationException extends RuntimeException {

    private final ShortPlaybackErrorCode errorCode;

    public ShortPlaybackApplicationException(ShortPlaybackErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ShortPlaybackErrorCode getErrorCode() {
        return errorCode;
    }
}
