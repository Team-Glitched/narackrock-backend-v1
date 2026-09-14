package glitched.adlips.application.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.media.port.out.MediaUploadSessionRepositoryPort;
import glitched.adlips.application.media.dto.request.MediaUploadSessionCreateRequest;
import glitched.adlips.application.media.dto.response.MediaUploadSessionCreateResponse;
import glitched.adlips.application.media.usecase.MediaUploadSessionCreateUseCase;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.media.MediaFileType;
import glitched.adlips.domain.media.MediaUploadSession;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

class MediaUploadSessionCreateUseCaseTest {
    @Test
    void createsUploadingMediaAndPutSession() {
        MediaFileRepositoryPort mediaFiles = mock(MediaFileRepositoryPort.class);
        MediaUploadSessionRepositoryPort sessions = mock(MediaUploadSessionRepositoryPort.class);
        when(mediaFiles.save(any(MediaFile.class))).thenAnswer(invocation -> invocation.<MediaFile>getArgument(0).withId(501L));
        when(sessions.save(any(MediaUploadSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Clock clock = Clock.fixed(Instant.parse("2026-07-05T10:00:00Z"), ZoneOffset.UTC);

        MediaUploadSessionCreateResponse response = new MediaUploadSessionCreateUseCase(
                mediaFiles, sessions, clock, "http://localhost:8080")
                .execute(new MediaUploadSessionCreateRequest(
                        1L, MediaFileType.AUDIO, "guitar.wav", "audio/wav", 10_240_000L));

        assertThat(response.mediaFileId()).isEqualTo(501L);
        assertThat(response.method()).isEqualTo("PUT");
        assertThat(response.uploadUrl()).isEqualTo("http://localhost:8080/api/v1/media/501/content");
        assertThat(response.expiresAt()).isEqualTo("2026-07-05T10:15:00");

        ArgumentCaptor<MediaFile> mediaCaptor = ArgumentCaptor.forClass(MediaFile.class);
        verify(mediaFiles).save(mediaCaptor.capture());
        assertThat(mediaCaptor.getValue().getStorageKey()).startsWith("audio/1/");
    }

    @ParameterizedTest
    @CsvSource({
            "IMAGE, cover.png, image/png, images",
            "AUDIO, guitar.wav, audio/wav, audio",
            "VIDEO, short.mp4, video/mp4, videos",
            "WAVEFORM, waveform.json, application/json, waveforms"
    })
    void groupsUploadedFilesByMediaType(
            MediaFileType fileType,
            String fileName,
            String mimeType,
            String expectedPrefix
    ) {
        MediaFileRepositoryPort mediaFiles = mock(MediaFileRepositoryPort.class);
        MediaUploadSessionRepositoryPort sessions = mock(MediaUploadSessionRepositoryPort.class);
        when(mediaFiles.save(any(MediaFile.class))).thenAnswer(
                invocation -> invocation.<MediaFile>getArgument(0).withId(503L));
        when(sessions.save(any(MediaUploadSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        new MediaUploadSessionCreateUseCase(
                mediaFiles, sessions, Clock.systemUTC(), "http://localhost:8080")
                .execute(new MediaUploadSessionCreateRequest(
                        7L, fileType, fileName, mimeType, 100L));

        ArgumentCaptor<MediaFile> mediaCaptor = ArgumentCaptor.forClass(MediaFile.class);
        verify(mediaFiles).save(mediaCaptor.capture());
        assertThat(mediaCaptor.getValue().getStorageKey())
                .startsWith(expectedPrefix + "/7/");
    }

    @Test
    void acceptsBrowserWaveMimeType() {
        MediaFileRepositoryPort mediaFiles = mock(MediaFileRepositoryPort.class);
        MediaUploadSessionRepositoryPort sessions = mock(MediaUploadSessionRepositoryPort.class);
        when(mediaFiles.save(any(MediaFile.class))).thenAnswer(
                invocation -> invocation.<MediaFile>getArgument(0).withId(502L));
        when(sessions.save(any(MediaUploadSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = new MediaUploadSessionCreateUseCase(
                mediaFiles, sessions, Clock.systemUTC(), "http://localhost:8080")
                .execute(new MediaUploadSessionCreateRequest(
                        1L, MediaFileType.AUDIO, "recording.wav", "audio/wave", 100L));

        assertThat(response.mediaFileId()).isEqualTo(502L);
    }
}
