package glitched.adlips.adapter.in.web.shorts;

import glitched.adlips.adapter.in.web.ApiErrorResponse;
import glitched.adlips.application.shorts.ShortsComposersApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ShortsComposersExceptionHandler {

    @ExceptionHandler(ShortsComposersApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handle(ShortsComposersApplicationException e) {
        HttpStatus status = switch (e.getErrorCode()) {
            case SHORTS_NOT_FOUND -> HttpStatus.NOT_FOUND;
        };
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(e.getErrorCode().name(), e.getMessage()));
    }
}
