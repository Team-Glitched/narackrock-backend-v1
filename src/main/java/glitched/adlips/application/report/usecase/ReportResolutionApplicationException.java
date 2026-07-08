package glitched.adlips.application.report.usecase;

public class ReportResolutionApplicationException extends RuntimeException {

    private final ReportResolutionErrorCode errorCode;

    public ReportResolutionApplicationException(ReportResolutionErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ReportResolutionErrorCode getErrorCode() {
        return errorCode;
    }
}
