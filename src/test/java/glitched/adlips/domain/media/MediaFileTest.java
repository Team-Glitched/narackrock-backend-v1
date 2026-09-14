package glitched.adlips.domain.media;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MediaFileTest {

    @Test
    void createsGeneratedArchiveAsReadyMedia() {
        MediaFile media = MediaFile.readyGenerated(
                1L, "/files/exports/10/layers.zip", "exports/10/layers.zip",
                "layers.zip", MediaFileType.ARCHIVE, "application/zip", 2048L);

        assertThat(media.getStatus()).isEqualTo(MediaFileStatus.READY);
        assertThat(media.getFileType()).isEqualTo(MediaFileType.ARCHIVE);
        assertThat(media.getFileSize()).isEqualTo(2048L);
    }
}
