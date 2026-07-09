package glitched.adlips.adapter.in.web.shorts;

import glitched.adlips.adapter.in.web.ApiErrorResponse;
import glitched.adlips.application.shorts.ShortCommentApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ShortCommentExceptionHandler {

    @ExceptionHandler(ShortCommentApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handle(ShortCommentApplicationException e) {
        HttpStatus status = switch (e.getErrorCode()) {
            case INVALID_INPUT_VALUE -> HttpStatus.BAD_REQUEST;
            case BANNED_USER_ACCESS -> HttpStatus.FORBIDDEN;
            case SHORT_NOT_FOUND, PARENT_COMMENT_NOT_FOUND -> HttpStatus.NOT_FOUND;
        };
        return ResponseEntity.status(status).body(ApiErrorResponse.of(e.getErrorCode().name(), e.getMessage()));
    }
}
