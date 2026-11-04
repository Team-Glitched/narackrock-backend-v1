package glitched.adlips.adapter.out.persistence.project;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.domain.project.EditorDraft;
import glitched.adlips.domain.project.EditorDraftSnapshot;
import glitched.adlips.domain.project.MidiNote;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class EditorDraftPersistenceAdapterTest {
    @Autowired EditorDraftJpaRepository repository;
    @Autowired EntityManager entityManager;

    @Test void roundTripsMidiJsonAndOnlyListsTheOwnersActiveDrafts() {
        var adapter = new EditorDraftPersistenceAdapter(repository);
        var content = new EditorDraftSnapshot(0, 480,
                List.of(new MidiNote(60, 0, 480, 100, "DELAY", Map.of("delayMs", 250))));
        var draft = EditorDraft.create(UUID.randomUUID(), 1L, 2L, 3L, 4L, content, content,
                UUID.randomUUID(), Instant.parse("2026-10-28T00:00:00Z"));
        adapter.save(draft);
        entityManager.flush(); entityManager.clear();
        assertThat(adapter.findById(draft.draftId())).contains(draft);
        assertThat(adapter.findActiveByProjectAndOwner(2, 9)).isEmpty();
        assertThat(adapter.findActiveByProjectAndOwner(2, 1)).containsExactly(draft);
        adapter.save(draft.commit(1, 4, draft.updatedAt()));
        entityManager.flush(); entityManager.clear();
        assertThat(adapter.findActiveByProjectAndOwner(2, 1)).isEmpty();
        assertThat(adapter.findById(draft.draftId()).orElseThrow().status())
                .isEqualTo(EditorDraft.Status.COMMITTED);
    }
}
