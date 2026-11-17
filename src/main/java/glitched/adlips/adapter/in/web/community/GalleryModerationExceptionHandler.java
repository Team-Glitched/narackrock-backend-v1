package glitched.adlips.adapter.in.web.community;

import glitched.adlips.adapter.in.web.dto.ApiResponse;
import glitched.adlips.application.community.GalleryModerationApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GalleryModerationExceptionHandler {

    @ExceptionHandler(GalleryModerationApplicationException.class)
    public ResponseEntity<ApiResponse<Void>> handle(GalleryModerationApplicationException e) {
        HttpStatus status = switch (e.getErrorCode()) {
            case VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case FORBIDDEN, CANNOT_SANCTION_TARGET -> HttpStatus.FORBIDDEN;
            case GALLERY_NOT_FOUND, GALLERY_BAN_NOT_FOUND, POST_NOT_FOUND, COMMENT_NOT_FOUND,
                 TARGET_USER_NOT_FOUND, GALLERY_MANAGER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ALREADY_BANNED_IN_GALLERY, ALREADY_GALLERY_MANAGER, CANNOT_GRANT_MANAGER,
                 LAST_GALLERY_MANAGER -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(status).body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
    }
}
