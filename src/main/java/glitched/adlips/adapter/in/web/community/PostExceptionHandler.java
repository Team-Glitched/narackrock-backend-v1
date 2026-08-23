package glitched.adlips.adapter.in.web.community;

import glitched.adlips.adapter.in.web.ApiErrorResponse;
import glitched.adlips.application.community.PostApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PostExceptionHandler {

    @ExceptionHandler(PostApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handle(PostApplicationException e) {
        HttpStatus status = switch (e.getErrorCode()) {
            case VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case NOT_POST_OWNER -> HttpStatus.FORBIDDEN;
            case GALLERY_NOT_FOUND, POST_NOT_FOUND -> HttpStatus.NOT_FOUND;
        };
        return ResponseEntity.status(status).body(ApiErrorResponse.of(e.getErrorCode().name(), e.getMessage()));
    }
}
