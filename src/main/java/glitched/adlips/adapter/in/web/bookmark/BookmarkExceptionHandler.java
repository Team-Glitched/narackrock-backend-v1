package glitched.adlips.adapter.in.web.bookmark;

import glitched.adlips.adapter.in.web.ApiErrorResponse;
import glitched.adlips.application.bookmark.BookmarkApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BookmarkExceptionHandler {

    @ExceptionHandler(BookmarkApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handle(BookmarkApplicationException e) {
        HttpStatus status = switch (e.getErrorCode()) {
            case INVALID_INPUT_VALUE -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(ApiErrorResponse.of(e.getErrorCode().name(), e.getMessage()));
    }
}
