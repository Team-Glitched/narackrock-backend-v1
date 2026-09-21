package glitched.adlips.adapter.in.web.community;

import glitched.adlips.adapter.in.web.dto.ApiResponse;
import glitched.adlips.application.community.GalleryApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GalleryExceptionHandler {

    @ExceptionHandler(GalleryApplicationException.class)
    public ResponseEntity<ApiResponse<Void>> handle(GalleryApplicationException e) {
        HttpStatus status = switch (e.getErrorCode()) {
            case GALLERY_NOT_FOUND, NOT_SUBSCRIBED -> HttpStatus.NOT_FOUND;
            case ALREADY_SUBSCRIBED -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(status).body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
    }
}
