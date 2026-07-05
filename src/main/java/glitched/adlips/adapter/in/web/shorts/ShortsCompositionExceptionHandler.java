package glitched.adlips.adapter.in.web.shorts;

import glitched.adlips.adapter.in.web.ApiErrorResponse;
import glitched.adlips.application.shorts.ShortsCompositionApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ShortsCompositionExceptionHandler {

    @ExceptionHandler(ShortsCompositionApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handle(ShortsCompositionApplicationException e) {
        HttpStatus status = switch (e.getErrorCode()) {
            case SHORT_NOT_FOUND, PROJECT_NOT_LINKED -> HttpStatus.NOT_FOUND;
        };
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(e.getErrorCode().name(), e.getMessage()));
    }
}
