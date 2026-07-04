package glitched.adlips.adapter.in.web.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.media.dto.request.MediaUploadSessionCreateRequest;
import glitched.adlips.application.media.dto.response.MediaUploadSessionCreateResponse;
import glitched.adlips.application.media.dto.response.MediaUploadCompleteResponse;
import glitched.adlips.application.media.usecase.LocalMediaContentUploadUseCase;
import glitched.adlips.application.media.usecase.MediaUploadCompleteUseCase;
import glitched.adlips.application.media.usecase.MediaUploadSessionCreateUseCase;
import glitched.adlips.domain.media.MediaFileType;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class MediaControllerTest {
    @Test
    void returnsCreatedUploadUrl() {
        MediaUploadSessionCreateUseCase createUseCase = mock(MediaUploadSessionCreateUseCase.class);
        LocalMediaContentUploadUseCase uploadUseCase = mock(LocalMediaContentUploadUseCase.class);
        MediaUploadCompleteUseCase completeUseCase = mock(MediaUploadCompleteUseCase.class);
        AuthenticatedUserResolver resolver = mock(AuthenticatedUserResolver.class);
        when(resolver.requireUserId("Bearer token")).thenReturn(1L);
        when(createUseCase.execute(any())).thenReturn(new MediaUploadSessionCreateResponse(
                501L, "http://localhost/api/v1/media/501/content", "PUT", "2026-07-05T10:15:00"));

        ResponseEntity<ApiResponse<MediaUploadSessionCreateResponse>> result =
                new MediaController(createUseCase, uploadUseCase, completeUseCase, resolver).createUploadSession(
                        "Bearer token", new MediaUploadSessionCreateRequest(
                                null, MediaFileType.AUDIO, "guitar.wav", "audio/wav", 100L));

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody().message()).isEqualTo("업로드 URL이 발급되었습니다.");
        assertThat(result.getBody().data().method()).isEqualTo("PUT");
    }

    @Test
    void returnsProcessingForUploadCompletion() {
        MediaUploadSessionCreateUseCase createUseCase = mock(MediaUploadSessionCreateUseCase.class);
        LocalMediaContentUploadUseCase uploadUseCase = mock(LocalMediaContentUploadUseCase.class);
        MediaUploadCompleteUseCase completeUseCase = mock(MediaUploadCompleteUseCase.class);
        AuthenticatedUserResolver resolver = mock(AuthenticatedUserResolver.class);
        when(resolver.requireUserId("Bearer token")).thenReturn(1L);
        when(completeUseCase.execute(any())).thenReturn(
                new MediaUploadCompleteResponse(501L, "PROCESSING"));

        var result = new MediaController(createUseCase, uploadUseCase, completeUseCase, resolver)
                .complete(501L, "Bearer token");

        assertThat(result.data().status()).isEqualTo("PROCESSING");
        assertThat(result.message()).isEqualTo("파일 업로드 완료 처리가 접수되었습니다.");
    }
}
