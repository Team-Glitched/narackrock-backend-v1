package glitched.adlips.application.shorts;

public class ShortsComposersApplicationException extends RuntimeException {

    private final ShortsComposersErrorCode errorCode;

    public ShortsComposersApplicationException(ShortsComposersErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ShortsComposersErrorCode getErrorCode() {
        return errorCode;
    }
}
