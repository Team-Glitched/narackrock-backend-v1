package glitched.adlips.application.shorts;

public class ShortsCompositionApplicationException extends RuntimeException {

    private final ShortsCompositionErrorCode errorCode;

    public ShortsCompositionApplicationException(ShortsCompositionErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ShortsCompositionErrorCode getErrorCode() {
        return errorCode;
    }
}
