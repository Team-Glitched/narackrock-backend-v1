package glitched.adlips.application.shorts;

public class ShortReportApplicationException extends RuntimeException {

    private final ShortReportErrorCode errorCode;

    public ShortReportApplicationException(ShortReportErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ShortReportErrorCode getErrorCode() {
        return errorCode;
    }
}
