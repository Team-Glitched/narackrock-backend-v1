package glitched.adlips.adapter.in.web.community;

import glitched.adlips.adapter.in.web.dto.ApiResponse;
import glitched.adlips.application.community.GalleryProposalApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GalleryProposalExceptionHandler {

    @ExceptionHandler(GalleryProposalApplicationException.class)
    public ResponseEntity<ApiResponse<Void>> handle(GalleryProposalApplicationException e) {
        HttpStatus status = switch (e.getErrorCode()) {
            case VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case FORBIDDEN, BANNED_USER_ACCESS -> HttpStatus.FORBIDDEN;
            case PROPOSAL_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case GALLERY_NAME_DUPLICATED, PROPOSAL_NAME_DUPLICATED, TOO_MANY_PENDING_PROPOSALS,
                 PROPOSAL_ALREADY_REVIEWED -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(status).body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
    }
}
