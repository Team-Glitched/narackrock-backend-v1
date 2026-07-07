package glitched.adlips.adapter.in.web.shorts;

import glitched.adlips.adapter.in.web.ApiErrorResponse;
import glitched.adlips.application.shorts.ShortReportApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ShortReportExceptionHandler {

    @ExceptionHandler(ShortReportApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handle(ShortReportApplicationException e) {
        HttpStatus status = switch (e.getErrorCode()) {
            case SHORT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CANNOT_REPORT_OWN_SHORT, VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case ALREADY_REPORTED -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(status).body(ApiErrorResponse.of(e.getErrorCode().name(), e.getMessage()));
    }
}
