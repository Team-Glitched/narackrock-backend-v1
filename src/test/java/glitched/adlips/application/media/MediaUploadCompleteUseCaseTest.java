package glitched.adlips.application.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.media.port.out.MediaContentStoragePort;
import glitched.adlips.application.media.port.out.MediaUploadSessionRepositoryPort;
import glitched.adlips.application.media.dto.request.MediaUploadCompleteRequest;
import glitched.adlips.application.media.usecase.MediaUploadCompleteUseCase;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.media.MediaFileStatus;
import glitched.adlips.domain.media.MediaFileType;
import glitched.adlips.domain.media.MediaUploadSession;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MediaUploadCompleteUseCaseTest {
    @Test
    void verifiesLocalObjectAndMarksItReady() {
        MediaFileRepositoryPort mediaFiles = mock(MediaFileRepositoryPort.class);
        MediaUploadSessionRepositoryPort sessions = mock(MediaUploadSessionRepositoryPort.class);
        MediaContentStoragePort storage = mock(MediaContentStoragePort.class);
        MediaFile media = MediaFile.restore(501L, 1L, null, "media/1/guitar.wav", "guitar.wav",
                MediaFileType.AUDIO, "audio/wav", 100L, MediaFileStatus.UPLOADING);
        MediaUploadSession session = new MediaUploadSession(
                501L, 1L, "url", 100L, "audio/wav", LocalDateTime.parse("2026-07-05T10:15:00"));
        when(mediaFiles.findById(501L)).thenReturn(Optional.of(media));
        when(sessions.findFirstByMediaFileIdOrderByCreatedAtDesc(501L)).thenReturn(Optional.of(session));
        when(storage.exists(media.getStorageKey())).thenReturn(true);
        when(storage.size(media.getStorageKey())).thenReturn(100L);
        when(storage.contentType(media.getStorageKey())).thenReturn("audio/wav");
        when(storage.publicUrl(media.getStorageKey())).thenReturn("http://localhost/files/media/1/guitar.wav");
        Clock clock = Clock.fixed(Instant.parse("2026-07-05T10:05:00Z"), ZoneOffset.UTC);

        var response = new MediaUploadCompleteUseCase(mediaFiles, sessions, storage, clock)
                .execute(new MediaUploadCompleteRequest(501L, 1L));

        assertThat(response.status()).isEqualTo("READY");
        verify(mediaFiles).save(org.mockito.ArgumentMatchers.argThat(
                saved -> saved.getStatus() == MediaFileStatus.READY && saved.getFileUrl() != null));
    }

    @Test
    void returnsReadyWhenCompletionIsRetried() {
        MediaFileRepositoryPort mediaFiles = mock(MediaFileRepositoryPort.class);
        MediaUploadSessionRepositoryPort sessions = mock(MediaUploadSessionRepositoryPort.class);
        MediaContentStoragePort storage = mock(MediaContentStoragePort.class);
        MediaFile ready = MediaFile.restore(501L, 1L, "http://localhost/files/guitar.wav",
                "media/1/guitar.wav", "guitar.wav", MediaFileType.AUDIO, "audio/wav", 100L,
                MediaFileStatus.READY);
        when(mediaFiles.findById(501L)).thenReturn(Optional.of(ready));

        var response = new MediaUploadCompleteUseCase(mediaFiles, sessions, storage, Clock.systemUTC())
                .execute(new MediaUploadCompleteRequest(501L, 1L));

        assertThat(response.status()).isEqualTo("READY");
    }
}
