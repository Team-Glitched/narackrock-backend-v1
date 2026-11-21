package glitched.adlips.adapter.in.web.community;

import glitched.adlips.adapter.in.web.ApiErrorResponse;
import glitched.adlips.application.report.usecase.ReportSubmissionApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PostReportExceptionHandler {

    @ExceptionHandler(ReportSubmissionApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handle(
            ReportSubmissionApplicationException exception
    ) {
        HttpStatus status = switch (exception.getErrorCode()) {
            case VALIDATION_ERROR, CANNOT_REPORT_OWN_POST, CANNOT_REPORT_OWN_OPEN_SOURCE, CANNOT_REPORT_OWN_COMMENT ->
                    HttpStatus.BAD_REQUEST;
            case BANNED_USER_ACCESS -> HttpStatus.FORBIDDEN;
            case POST_NOT_FOUND, OPEN_SOURCE_NOT_FOUND, COMMENT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ALREADY_REPORTED -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(status).body(ApiErrorResponse.of(
                exception.getErrorCode().name(), exception.getMessage()));
    }
}
