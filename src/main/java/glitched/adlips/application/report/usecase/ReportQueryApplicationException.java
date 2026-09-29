package glitched.adlips.application.report.usecase;

public class ReportQueryApplicationException extends RuntimeException {

    private final ReportQueryErrorCode errorCode;

    public ReportQueryApplicationException(
            ReportQueryErrorCode errorCode,
            String message
    ) {
        super(message);
        this.errorCode = errorCode;
    }

    public ReportQueryErrorCode getErrorCode() {
        return errorCode;
    }
}
