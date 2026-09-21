package glitched.adlips.application.report.usecase;

public class ReportSubmissionApplicationException extends RuntimeException {

    private final ReportSubmissionErrorCode errorCode;

    public ReportSubmissionApplicationException(
            ReportSubmissionErrorCode errorCode,
            String message
    ) {
        super(message);
        this.errorCode = errorCode;
    }

    public ReportSubmissionErrorCode getErrorCode() {
        return errorCode;
    }
}
