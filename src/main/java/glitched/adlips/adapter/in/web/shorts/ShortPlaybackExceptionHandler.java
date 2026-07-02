package glitched.adlips.adapter.in.web.shorts;

import glitched.adlips.adapter.in.web.ApiErrorResponse;
import glitched.adlips.application.shorts.ShortPlaybackApplicationException;
import glitched.adlips.application.shorts.ShortPlaybackErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ShortPlaybackExceptionHandler {

    @ExceptionHandler(ShortPlaybackApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handle(ShortPlaybackApplicationException e) {
        HttpStatus status = switch (e.getErrorCode()) {
            case SHORT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_PLAYBACK_STATE, INVALID_CURRENT_TIME -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(e.getErrorCode().name(), e.getMessage()));
    }
}
