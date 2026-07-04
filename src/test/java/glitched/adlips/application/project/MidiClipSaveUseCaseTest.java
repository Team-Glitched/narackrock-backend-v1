package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.adapter.out.persistence.project.ProjectClipJpaRepository;
import glitched.adlips.application.project.dto.request.MidiClipSaveRequest;
import glitched.adlips.application.project.dto.request.MidiNoteRequest;
import glitched.adlips.application.project.usecase.MidiClipSaveUseCase;
import glitched.adlips.domain.project.MidiNote;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectClip;
import glitched.adlips.domain.project.ProjectTrack;
import glitched.adlips.domain.user.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MidiClipSaveUseCaseTest {
    @Test
    void updatesOwnedMidiClipAndInvalidatesTrackRender() {
        User owner = User.create("owner@example.com").withId(1L);
        Project project = new Project(owner, "곡", null, 701L);
        ProjectTrack track = new ProjectTrack(project, owner, "Piano", "PIANO", 0);
        ProjectClip clip = ProjectClip.createMidi(project, track, owner, 0, 1920, List.of(), 0);
        track.replaceRenderedMedia(900L);
        ProjectClipJpaRepository clips = mock(ProjectClipJpaRepository.class);
        when(clips.findByIdAndIsDeletedFalse(1003L)).thenReturn(Optional.of(clip));
        when(clips.save(any(ProjectClip.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MidiNoteRequest note = new MidiNoteRequest(60, 0, 480, 100, "DELAY", Map.of("delayMs", 250));
        Clock clock = Clock.fixed(Instant.parse("2026-07-05T10:00:00Z"), ZoneOffset.UTC);

        var response = new MidiClipSaveUseCase(clips, clock)
                .execute(new MidiClipSaveRequest(1003L, 1L, 0, 1920, List.of(note)));

        assertThat(response.noteCount()).isEqualTo(1);
        assertThat(response.trackMediaFileId()).isNull();
        assertThat(response.updatedAt()).isEqualTo("2026-07-05T10:00:00");
        assertThat(clip.getMidiNotes()).containsExactly(
                new MidiNote(60, 0, 480, 100, "DELAY", Map.of("delayMs", 250)));
    }
}
