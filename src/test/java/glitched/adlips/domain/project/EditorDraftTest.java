package glitched.adlips.domain.project;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EditorDraftTest {
    private final Instant now = Instant.parse("2026-10-28T00:00:00Z");
    private final EditorDraftSnapshot notes = new EditorDraftSnapshot(0, 480,
            List.of(new MidiNote(60, 0, 480, 100, null, Map.of())));

    private EditorDraft draft() {
        return EditorDraft.create(UUID.randomUUID(), 1L, 2L, 3L, null, notes, null,
                UUID.randomUUID(), now);
    }

    @Test void savesEmptyNotesAsAnIntentionalDeletion() {
        var draft = draft();
        var empty = new EditorDraftSnapshot(0, 480, List.of());
        assertThat(draft.update(1, UUID.randomUUID(), empty, now).content().midiNotes()).isEmpty();
    }

    @Test void rejectsStaleUpdatesInsteadOfOverwritingAnotherDevice() {
        var draft = draft().update(1, UUID.randomUUID(), notes, now);
        assertThatThrownBy(() -> draft.update(1, UUID.randomUUID(), notes, now))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void retriesTheSameMutationWithoutIncrementingRevision() {
        var draft = draft();
        assertThat(draft.update(0, draft.mutationId(), notes, now)).isSameAs(draft);
    }

    @Test void cannotReuseMutationIdWithDifferentContent() {
        var draft = draft();
        assertThatThrownBy(() -> draft.update(1, draft.mutationId(),
                new EditorDraftSnapshot(0, 480, List.of()), now))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void committedDraftRemainsAsATombstoneAgainstLateAutosaves() {
        var draft = draft().commit(1, 99L, now);
        assertThat(draft.status()).isEqualTo(EditorDraft.Status.COMMITTED);
        assertThat(draft.committedClipId()).isEqualTo(99L);
        assertThatThrownBy(() -> draft.update(2, UUID.randomUUID(), notes, now))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void staleCommitAndDiscardAreRejected() {
        var draft = draft();
        assertThatThrownBy(() -> draft.commit(0, 99L, now)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> draft.discard(0, now)).isInstanceOf(IllegalStateException.class);
    }

    @Test void validatesTimelineAndNoteBounds() {
        assertThatThrownBy(() -> new EditorDraftSnapshot(-1, 480, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EditorDraftSnapshot(0, 240, notes.midiNotes()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> notes.validateMaxTick(240))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
