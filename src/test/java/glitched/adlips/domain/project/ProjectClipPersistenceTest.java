package glitched.adlips.domain.project;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.domain.user.User;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Transactional
class ProjectClipPersistenceTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAndRestoresStructuredMidiNotesAsJson() {
        User owner = new User("midi-owner@example.com");
        Project project = new Project(owner, "MIDI 곡", "설명", 701L);
        ProjectTrack track = new ProjectTrack(project, owner, "피아노", "PIANO", 0);
        MidiNote note = new MidiNote(
                60, 0, 480, 100, "DELAY", Map.of("delayMs", 250));
        ProjectClip clip = ProjectClip.createMidi(
                project, track, owner, 0, 1_920, List.of(note), 0);

        entityManager.persist(owner);
        entityManager.persist(project);
        entityManager.persist(track);
        entityManager.persist(clip);
        entityManager.flush();
        Long clipId = clip.getId();
        entityManager.clear();

        ProjectClip restored = entityManager.find(ProjectClip.class, clipId);

        assertThat(restored.getMidiNotes()).containsExactly(note);
    }
}
