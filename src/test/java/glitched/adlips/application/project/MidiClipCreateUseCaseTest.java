package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.adapter.out.persistence.project.ProjectClipJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectMemberJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectTrackJpaRepository;
import glitched.adlips.application.project.dto.request.MidiClipCreateRequest;
import glitched.adlips.application.project.usecase.MidiClipCreateUseCase;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.project.MidiNote;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectClip;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.project.ProjectTrack;
import glitched.adlips.domain.user.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MidiClipCreateUseCaseTest {
    @Test
    void createsDraftMidiClipWithTrackInstrument() {
        User owner = User.create("owner@example.com").withId(1L);
        Project project = new Project(owner, "곡", null, 701L);
        ProjectTrack track = new ProjectTrack(project, owner, "Piano", "PIANO", 0);
        ProjectJpaRepository projects = mock(ProjectJpaRepository.class);
        ProjectMemberJpaRepository members = mock(ProjectMemberJpaRepository.class);
        ProjectTrackJpaRepository tracks = mock(ProjectTrackJpaRepository.class);
        ProjectClipJpaRepository clips = mock(ProjectClipJpaRepository.class);
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        when(projects.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 1L)).thenReturn(
                Optional.of(new ProjectMember(project, owner, ProjectMemberRole.OWNER)));
        when(tracks.findByIdAndIsDeletedFalse(103L)).thenReturn(Optional.of(track));
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(clips.save(any(ProjectClip.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MidiNote note = new MidiNote(60, 0, 480, 100, "DELAY", Map.of("delayMs", 250));

        var response = new MidiClipCreateUseCase(projects, members, tracks, clips, users)
                .execute(new MidiClipCreateRequest(10L, 103L, 1L, 0, 1920, List.of(note)));

        assertThat(response.clipType().name()).isEqualTo("MIDI");
        assertThat(response.instrument()).isEqualTo("PIANO");
        assertThat(response.midiNotes()).containsExactly(note);
        assertThat(response.durationMs()).isEqualTo(2000);
    }
}
