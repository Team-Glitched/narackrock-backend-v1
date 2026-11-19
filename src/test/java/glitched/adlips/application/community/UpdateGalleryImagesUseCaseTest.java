package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import glitched.adlips.application.community.port.out.*;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.community.*;
import glitched.adlips.domain.media.*;
import glitched.adlips.domain.user.UserRole;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateGalleryImagesUseCaseTest {
    final GalleryAuthorityPort authority = mock(GalleryAuthorityPort.class);
    final GalleryManagementPort management = mock(GalleryManagementPort.class);
    final GalleryModerationLogPort logs = mock(GalleryModerationLogPort.class);
    final MediaFileRepositoryPort media = mock(MediaFileRepositoryPort.class);
    final Clock clock = Clock.fixed(Instant.parse("2026-11-19T09:00:00Z"), ZoneOffset.UTC);
    final Gallery gallery = Gallery.create("기타 커뮤니티", "기타 연주 이야기");
    final UpdateGalleryImagesUseCase useCase = new UpdateGalleryImagesUseCase(authority, management, logs, media, TransactionRunner.direct(), clock);
    @BeforeEach void setup() {
        when(management.findGalleryForUpdate(10L)).thenReturn(Optional.of(gallery));
        when(authority.findActor(3L, 10L, LocalDateTime.now(clock)))
            .thenReturn(Optional.of(new GalleryActor(3L, UserRole.USER, GalleryRole.MANAGER, false)));
    }
    @Test void managerSavesOwnedReadyImagesAndLog() {
        when(media.findById(11L)).thenReturn(Optional.of(image(3L)));
        when(media.findById(12L)).thenReturn(Optional.of(image(3L)));
        useCase.execute(3L, 10L, 11L, 12L);
        assertThat(gallery.getImageFileId()).isEqualTo(11L);
        assertThat(gallery.getBannerImageFileId()).isEqualTo(12L);
        assertThat(gallery.getDescription()).isEqualTo("기타 연주 이야기");
        verify(management).updateGallery(gallery);
        verify(logs).save(10L, 3L, GalleryModerationActionType.UPDATE_GALLERY, null, 10L, null);
    }
    @Test void nonManagerCannotUpdateImages() {
        when(authority.findActor(3L,10L,LocalDateTime.now(clock)))
            .thenReturn(Optional.of(new GalleryActor(3L,UserRole.USER,null,false)));
        assertThatThrownBy(() -> useCase.execute(3L,10L,11L,null))
            .isInstanceOf(GalleryModerationApplicationException.class).extracting("errorCode").isEqualTo(GalleryModerationErrorCode.FORBIDDEN);
        verifyNoInteractions(media);
        verify(management,never()).updateGallery(any());
    }
    @Test void invalidBannerDoesNotPartiallyUpdateIcon() {
        when(media.findById(11L)).thenReturn(Optional.of(image(3L)));
        when(media.findById(12L)).thenReturn(Optional.of(image(9L)));
        assertThatThrownBy(() -> useCase.execute(3L,10L,11L,12L)).isInstanceOf(GalleryModerationApplicationException.class);
        assertThat(gallery.getImageFileId()).isNull();
        verify(management,never()).updateGallery(any());
        verifyNoInteractions(logs);
    }
    @Test void preservesExistingImagesFromAnotherManagerAndAllowsRemoval() {
        gallery.changeImages(11L,12L);
        useCase.execute(3L,10L,11L,12L);
        verifyNoInteractions(media);
        useCase.execute(3L,10L,null,null);
        assertThat(gallery.getImageFileId()).isNull();
        assertThat(gallery.getBannerImageFileId()).isNull();
    }
    @Test void rejectsMissingNonImageOrIncompleteMedia() {
        for(MediaFile file: new MediaFile[]{
            MediaFile.uploading(3L,"upload","image.png",MediaFileType.IMAGE,"image/png",100),
            MediaFile.readyGenerated(3L,"http://files/audio.wav","audio","audio.wav",MediaFileType.AUDIO,"audio/wav",100)
        }) {
            when(media.findById(11L)).thenReturn(Optional.of(file));
            assertThatThrownBy(() -> useCase.execute(3L,10L,11L,null)).isInstanceOf(GalleryModerationApplicationException.class);
        }
        when(media.findById(11L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.execute(3L,10L,11L,null)).isInstanceOf(GalleryModerationApplicationException.class);
        assertThatThrownBy(() -> useCase.execute(3L,10L,-1L,null)).isInstanceOf(GalleryModerationApplicationException.class);
        verify(management,never()).updateGallery(any());
    }
    private MediaFile image(Long owner) { return MediaFile.readyImage(owner,"http://files/image.png","image","image.png","image/png",100); }
}
