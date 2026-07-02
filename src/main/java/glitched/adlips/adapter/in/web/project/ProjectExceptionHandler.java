package glitched.adlips.adapter.in.web.project;

import glitched.adlips.adapter.in.web.ApiErrorResponse;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProjectExceptionHandler {
    @ExceptionHandler(ProjectApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handle(ProjectApplicationException exception) {
        return ResponseEntity.status(statusOf(exception.getErrorCode())).body(ApiErrorResponse.of(
                exception.getErrorCode().name(), exception.getMessage()));
    }

    private HttpStatus statusOf(ProjectErrorCode code) {
        return switch (code) {
            case PROJECT_NOT_FOUND, TRACK_NOT_FOUND, MEDIA_FILE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case PROJECT_ACCESS_DENIED, TRACK_EDIT_DENIED, TRACK_DELETE_DENIED, MEDIA_FILE_ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case MEDIA_FILE_NOT_READY, TRACK_HAS_PENDING_CONTRIBUTION, EXPORT_TARGET_NOT_FOUND -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
