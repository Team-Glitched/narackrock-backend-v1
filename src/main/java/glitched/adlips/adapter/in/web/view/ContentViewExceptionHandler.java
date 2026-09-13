package glitched.adlips.adapter.in.web.view;

import glitched.adlips.adapter.in.web.ApiErrorResponse;
import glitched.adlips.application.view.ContentViewApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ContentViewExceptionHandler {

    @ExceptionHandler(ContentViewApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handle(ContentViewApplicationException exception) {
        HttpStatus status = switch (exception.getErrorCode()) {
            case CONTENT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case VIEW_COUNT_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case INVALID_CONTENT_ID, VIEWER_ID_REQUIRED -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(ApiErrorResponse.of(
                exception.getErrorCode().name(), exception.getMessage()));
    }
}
