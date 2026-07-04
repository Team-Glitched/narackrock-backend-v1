package glitched.adlips.adapter.in.web.media;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.media.dto.request.MediaUploadSessionCreateRequest;
import glitched.adlips.application.media.dto.response.MediaUploadSessionCreateResponse;
import glitched.adlips.application.media.usecase.LocalMediaContentUploadUseCase;
import glitched.adlips.application.media.usecase.MediaUploadSessionCreateUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {
    private final MediaUploadSessionCreateUseCase createUseCase;
    private final LocalMediaContentUploadUseCase contentUploadUseCase;
    private final AuthenticatedUserResolver userResolver;

    public MediaController(MediaUploadSessionCreateUseCase createUseCase,
                           LocalMediaContentUploadUseCase contentUploadUseCase,
                           AuthenticatedUserResolver userResolver) {
        this.createUseCase = createUseCase;
        this.contentUploadUseCase = contentUploadUseCase;
        this.userResolver = userResolver;
    }

    @PostMapping("/upload-sessions")
    public ResponseEntity<ApiResponse<MediaUploadSessionCreateResponse>> createUploadSession(
            @RequestHeader("Authorization") String authorization,
            @RequestBody MediaUploadSessionCreateRequest request) {
        Long userId = userResolver.requireUserId(authorization);
        var response = createUseCase.execute(new MediaUploadSessionCreateRequest(
                userId, request.fileType(), request.fileName(), request.mimeType(), request.fileSize()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("업로드 URL이 발급되었습니다.", response));
    }

    @PutMapping("/{mediaFileId}/content")
    public ResponseEntity<Void> uploadContent(
            @PathVariable Long mediaFileId,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("Content-Type") String contentType,
            @RequestBody byte[] content) {
        contentUploadUseCase.execute(mediaFileId, userResolver.requireUserId(authorization), contentType, content);
        return ResponseEntity.noContent().build();
    }
}
