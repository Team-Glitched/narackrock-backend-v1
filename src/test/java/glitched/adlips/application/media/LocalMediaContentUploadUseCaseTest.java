package glitched.adlips.application.media;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.adapter.out.persistence.media.MediaUploadSessionJpaRepository;
import glitched.adlips.adapter.out.storage.LocalFileStorageAdapter;
import glitched.adlips.application.media.usecase.LocalMediaContentUploadUseCase;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.media.MediaFileStatus;
import glitched.adlips.domain.media.MediaFileType;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LocalMediaContentUploadUseCaseTest {
    @Test
    void rejectsOverwriteAfterUploadCompletion() {
        MediaFileRepositoryPort mediaFiles = mock(MediaFileRepositoryPort.class);
        MediaUploadSessionJpaRepository sessions = mock(MediaUploadSessionJpaRepository.class);
        LocalFileStorageAdapter storage = mock(LocalFileStorageAdapter.class);
        MediaFile ready = MediaFile.restore(501L, 1L, "http://localhost/files/guitar.wav",
                "media/1/guitar.wav", "guitar.wav", MediaFileType.AUDIO, "audio/wav", 100L,
                MediaFileStatus.READY);
        when(mediaFiles.findById(501L)).thenReturn(Optional.of(ready));

        assertThatThrownBy(() -> new LocalMediaContentUploadUseCase(mediaFiles, sessions, storage)
                .execute(501L, 1L, "audio/wav", new byte[]{1}))
                .isInstanceOf(MediaApplicationException.class)
                .extracting(exception -> ((MediaApplicationException) exception).getErrorCode())
                .isEqualTo(MediaErrorCode.MEDIA_FILE_NOT_READY);
    }
}
