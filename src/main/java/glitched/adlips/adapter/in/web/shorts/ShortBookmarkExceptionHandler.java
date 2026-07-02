package glitched.adlips.adapter.in.web.shorts;

import glitched.adlips.adapter.in.web.ApiErrorResponse;
import glitched.adlips.application.shorts.ShortBookmarkApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ShortBookmarkExceptionHandler {

    @ExceptionHandler(ShortBookmarkApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handle(ShortBookmarkApplicationException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrorResponse.of(
                exception.getErrorCode().name(),
                exception.getMessage()
        ));
    }
}
