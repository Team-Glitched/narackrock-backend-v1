package glitched.adlips.adapter.in.web.community;

import glitched.adlips.adapter.in.web.ApiErrorResponse;
import glitched.adlips.application.community.PostCommentApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PostCommentExceptionHandler {

    @ExceptionHandler(PostCommentApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handle(PostCommentApplicationException e) {
        HttpStatus status = switch (e.getErrorCode()) {
            case INVALID_INPUT_VALUE -> HttpStatus.BAD_REQUEST;
            case BANNED_USER_ACCESS, NOT_COMMENT_OWNER -> HttpStatus.FORBIDDEN;
            case POST_NOT_FOUND, PARENT_COMMENT_NOT_FOUND, COMMENT_NOT_FOUND -> HttpStatus.NOT_FOUND;
        };
        return ResponseEntity.status(status).body(ApiErrorResponse.of(e.getErrorCode().name(), e.getMessage()));
    }
}
