package glitched.adlips.application.shorts;

public class ShortReactionApplicationException extends RuntimeException {

    private final ShortReactionErrorCode errorCode;

    public ShortReactionApplicationException(ShortReactionErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ShortReactionErrorCode getErrorCode() {
        return errorCode;
    }
}
