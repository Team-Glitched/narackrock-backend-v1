package glitched.adlips.application.opensource;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.*;
import glitched.adlips.domain.opensource.OpenSourcePublication;
import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OpenSourcePublicationUseCaseTest {
    final OpenSourcePublicationPort publications = mock(OpenSourcePublicationPort.class);
    final MediaFileRepositoryPort media = mock(MediaFileRepositoryPort.class);
    final Clock clock = Clock.fixed(Instant.parse("2026-10-28T00:00:00Z"), ZoneOffset.UTC);
    final OpenSourcePublicationUseCase useCase = new OpenSourcePublicationUseCase(publications, media, TransactionRunner.direct(), clock);
    OpenSourcePublication source;

    @BeforeEach void setup() {
        source = new OpenSourcePublication(10L, 1L, "title", "description", 20L, 30L, false, clock.instant(), clock.instant(), null, null);
        when(publications.findOwned(10L, 1L, true)).thenReturn(Optional.of(source));
        when(publications.findOwned(10L, 1L, false)).thenReturn(Optional.of(source));
        when(publications.save(any())).thenAnswer(call -> call.getArgument(0));
        when(media.findById(20L)).thenReturn(Optional.of(file(20L, 1L, MediaFileType.AUDIO, MediaFileStatus.READY)));
        when(media.findById(30L)).thenReturn(Optional.of(file(30L, 1L, MediaFileType.IMAGE, MediaFileStatus.READY)));
    }
    @Test void registrationIsPrivateAndTrimsTitle() {
        var result = useCase.create(1L, " title ", "desc", 20L, 30L, null);
        assertThat(result.isPublic()).isFalse();
        assertThat(result.title()).isEqualTo("title");
        assertThat(result.ownerId()).isEqualTo(1L);
    }
    @Test void foreignMediaIsNotFound() {
        when(media.findById(20L)).thenReturn(Optional.of(file(20L, 2L, MediaFileType.AUDIO, MediaFileStatus.READY)));
        assertCode(() -> useCase.create(1L, "title", null, 20L, null, null), "MEDIA_FILE_NOT_FOUND");
        verify(publications, never()).save(any());
    }
    @Test void unfinishedAndWrongTypeMediaAreRejected() {
        when(media.findById(20L)).thenReturn(Optional.of(file(20L, 1L, MediaFileType.AUDIO, MediaFileStatus.UPLOADING)));
        assertCode(() -> useCase.create(1L, "title", null, 20L, null, null), "MEDIA_FILE_NOT_READY");
        when(media.findById(20L)).thenReturn(Optional.of(file(20L, 1L, MediaFileType.IMAGE, MediaFileStatus.READY)));
        assertCode(() -> useCase.create(1L, "title", null, 20L, null, null), "INVALID_REQUEST");
    }
    @Test void invalidMetadataIsRejected() {
        assertCode(() -> useCase.create(1L, " ", null, 20L, null, null), "INVALID_REQUEST");
        assertCode(() -> useCase.create(1L, "x".repeat(41), null, 20L, null, null), "INVALID_REQUEST");
        assertCode(() -> useCase.create(1L, "title", "x".repeat(501), 20L, null, null), "INVALID_REQUEST");
        var boundary = useCase.create(1L, "x".repeat(40), "d".repeat(500), 20L, null, null);
        assertThat(boundary.title()).hasSize(40);
        assertThat(boundary.description()).hasSize(500);
    }
    @Test void omittedFieldsArePreservedAndExplicitNullClearsOptionalFields() {
        var patch = new OpenSourcePublicationUseCase.Patch(false, null, true, null, true, null, false, null);
        var updated = useCase.update(1L, 10L, patch);
        assertThat(updated.title()).isEqualTo("title");
        assertThat(updated.description()).isNull();
        assertThat(updated.thumbnailMediaFileId()).isNull();
        assertThat(updated.mediaFileId()).isEqualTo(20L);
    }
    @Test void emptyPatchAndNullTitleAreRejected() {
        assertCode(() -> useCase.update(1L, 10L, new OpenSourcePublicationUseCase.Patch(false,null,false,null,false,null, false, null)), "INVALID_REQUEST");
        assertCode(() -> useCase.update(1L, 10L, new OpenSourcePublicationUseCase.Patch(true,null,false,null,false,null, false, null)), "INVALID_REQUEST");
    }
    @Test void omittedLegacyTextIsPreservedButSuppliedTextIsStillLimited() {
        var legacy = new OpenSourcePublication(11L, 1L, "t".repeat(80), "d".repeat(1200), 20L, 30L, false, clock.instant(), clock.instant(), null, null);
        when(publications.findOwned(11L, 1L, true)).thenReturn(Optional.of(legacy));
        var updated = useCase.update(1L, 11L, new OpenSourcePublicationUseCase.Patch(false, null, false, null, false, null, true, "violin"));
        assertThat(updated.instrument()).isEqualTo("violin");
        assertThat(updated.title()).isEqualTo("t".repeat(80));
        assertThat(updated.description()).isEqualTo("d".repeat(1200));
        assertCode(() -> useCase.update(1L, 11L, new OpenSourcePublicationUseCase.Patch(false, null, true, "x".repeat(501), false, null, false, null)), "INVALID_REQUEST");
        assertThat(useCase.update(1L, 11L, new OpenSourcePublicationUseCase.Patch(false, null, true, "😀".repeat(500), false, null, false, null)).description()).isEqualTo("😀".repeat(500));
    }
    @Test void publishingRechecksMediaAndWithdrawalDoesNotRequireReadyMedia() {
        assertThat(useCase.visibility(1L, 10L, true).isPublic()).isTrue();
        var legacy = new OpenSourcePublication(11L, 1L, "t".repeat(80), "d".repeat(1200), 20L, 30L, false, clock.instant(), clock.instant(), null, null);
        when(publications.findOwned(11L, 1L, true)).thenReturn(Optional.of(legacy));
        assertThat(useCase.visibility(1L, 11L, true).isPublic()).isTrue();
        when(media.findById(20L)).thenReturn(Optional.empty());
        assertCode(() -> useCase.visibility(1L, 10L, true), "MEDIA_FILE_NOT_FOUND");
        assertThat(useCase.visibility(1L, 10L, false).isPublic()).isFalse();
    }
    @Test void foreignOwnerCannotReadOrMutate() {
        assertCode(() -> useCase.get(2L, 10L), "OPEN_SOURCE_NOT_FOUND");
        assertCode(() -> useCase.visibility(2L, 10L, true), "OPEN_SOURCE_NOT_FOUND");
        assertCode(() -> useCase.delete(2L, 10L), "OPEN_SOURCE_NOT_FOUND");
    }
    @Test void deletionIsIdempotentAndDeletedSourceCannotBeReadOrPublished() {
        var deleted = useCase.delete(1L, 10L);
        assertThat(deleted.deletedAt()).isEqualTo(clock.instant());
        when(publications.findOwned(10L, 1L, true)).thenReturn(Optional.of(deleted));
        when(publications.findOwned(10L, 1L, false)).thenReturn(Optional.of(deleted));
        assertThat(useCase.delete(1L, 10L).deletedAt()).isEqualTo(deleted.deletedAt());
        assertCode(() -> useCase.get(1L, 10L), "OPEN_SOURCE_NOT_FOUND");
        assertCode(() -> useCase.visibility(1L, 10L, true), "OPEN_SOURCE_NOT_FOUND");
    }
    @Test void optionalMidiNotesAreValidatedAndSerializedOnCreation() {
        var note = new OpenSourcePublicationUseCase.MidiNoteData(60, 0, 480, 100);
        var created = useCase.create(1L, "midi riff", "desc", 20L, 30L, "piano", List.of(note), null, 120);
        assertThat(created.hasMidiNotes()).isTrue();
        assertThat(created.midiPpq()).isEqualTo(480);
        assertThat(created.midiBpm()).isEqualTo(120);
        assertThat(created.midiNotesJson()).contains("\"pitch\":60", "\"startTick\":0", "\"durationTick\":480", "\"velocity\":100");

        var audioOnly = useCase.create(1L, "audio riff", "desc", 20L, null, "piano");
        assertThat(audioOnly.hasMidiNotes()).isFalse();
        assertThat(audioOnly.midiNotesJson()).isNull();

        assertCode(() -> useCase.create(1L, "x", null, 20L, null, null, List.of(), 480, 120), "INVALID_REQUEST");
        assertCode(() -> useCase.create(1L, "x", null, 20L, null, null, null, 480, 120), "INVALID_REQUEST");
        assertCode(() -> useCase.create(1L, "x", null, 20L, null, null,
                List.of(new OpenSourcePublicationUseCase.MidiNoteData(128, 0, 480, 100)), 480, 120), "INVALID_REQUEST");
        assertCode(() -> useCase.create(1L, "x", null, 20L, null, null,
                List.of(new OpenSourcePublicationUseCase.MidiNoteData(60, 0, 0, 100)), 480, 120), "INVALID_REQUEST");
        assertCode(() -> useCase.create(1L, "x", null, 20L, null, null,
                List.of(note), 480, 29), "INVALID_REQUEST");
    }
    static MediaFile file(long id, long owner, MediaFileType type, MediaFileStatus status) {
        return MediaFile.restore(id, owner, "http://local/file", "private/key", "test", type, "audio/wav", 100, status);
    }
    static void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOf(OpenSourceException.class)
            .satisfies(e -> assertThat(((OpenSourceException)e).getCode().name()).isEqualTo(code));
    }
}
