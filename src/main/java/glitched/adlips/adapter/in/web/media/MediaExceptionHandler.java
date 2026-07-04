package glitched.adlips.adapter.in.web.media;

import glitched.adlips.adapter.in.web.ApiErrorResponse;
import glitched.adlips.application.media.MediaApplicationException;
import glitched.adlips.application.media.MediaErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MediaExceptionHandler {
    @ExceptionHandler(MediaApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handle(MediaApplicationException exception) {
        return ResponseEntity.status(statusOf(exception.getErrorCode())).body(new ApiErrorResponse(
                false, exception.getErrorCode().name(), exception.getMessage(), null));
    }

    private HttpStatus statusOf(MediaErrorCode code) {
        return switch (code) {
            case MEDIA_FILE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case MEDIA_FILE_ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case UPLOAD_SESSION_EXPIRED, UPLOADED_OBJECT_NOT_FOUND, MEDIA_FILE_NOT_READY -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
