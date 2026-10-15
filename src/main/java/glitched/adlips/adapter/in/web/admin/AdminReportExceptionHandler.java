package glitched.adlips.adapter.in.web.admin;

import glitched.adlips.adapter.in.web.ApiErrorResponse;
import glitched.adlips.application.report.usecase.ReportResolutionApplicationException;
import glitched.adlips.application.report.usecase.ReportQueryApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AdminReportExceptionHandler {

    @ExceptionHandler(ReportQueryApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handle(ReportQueryApplicationException e) {
        HttpStatus status = switch (e.getErrorCode()) {
            case VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
        };
        return ResponseEntity.status(status).body(
                ApiErrorResponse.of(e.getErrorCode().name(), e.getMessage()));
    }

    @ExceptionHandler(ReportResolutionApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handle(ReportResolutionApplicationException e) {
        HttpStatus status = switch (e.getErrorCode()) {
            case VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case REPORT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ALREADY_HANDLED, ALREADY_BANNED_USER -> HttpStatus.CONFLICT;
            case TARGET_NOT_FOUND -> HttpStatus.NOT_FOUND;
        };
        return ResponseEntity.status(status).body(ApiErrorResponse.of(e.getErrorCode().name(), e.getMessage()));
    }
}
