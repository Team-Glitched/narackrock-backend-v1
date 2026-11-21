package glitched.adlips.application.report.usecase;

public enum ReportSubmissionErrorCode {
    VALIDATION_ERROR,
    POST_NOT_FOUND,
    OPEN_SOURCE_NOT_FOUND,
    COMMENT_NOT_FOUND,
    CANNOT_REPORT_OWN_POST,
    CANNOT_REPORT_OWN_OPEN_SOURCE,
    CANNOT_REPORT_OWN_COMMENT,
    BANNED_USER_ACCESS,
    ALREADY_REPORTED
}
