package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import glitched.adlips.application.project.dto.request.HtiMidiTransformRequest;
import glitched.adlips.application.project.port.out.HtiMidiTransformPort;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.usecase.HtiMidiTransformUseCase;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.user.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HtiMidiTransformUseCaseTest {
    @Test
    void transformsAudioWithProjectTempoForEditableMember() {
        User owner = User.create("owner@example.com").withId(1L);
        Project project = new Project(owner, "곡", null, 701L);
        ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
        ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
        HtiMidiTransformPort transformer = mock(HtiMidiTransformPort.class);
        byte[] content = {1, 2};
        when(projects.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 1L)).thenReturn(
                Optional.of(new ProjectMember(project, owner, ProjectMemberRole.OWNER)));
        var note = new HtiMidiTransformPort.Note(60, 0, 480, 100, null, Map.of());
        when(transformer.transform(new HtiMidiTransformPort.Command(
                10L, "recording.wav", "audio/wav", content, 120, 480)))
                .thenReturn(new HtiMidiTransformPort.Result(960, List.of(note)));

        var response = new HtiMidiTransformUseCase(projects, members, transformer).execute(
                new HtiMidiTransformRequest(10L, 1L, "recording.wav", "audio/wav", content));

        assertThat(response.durationTick()).isEqualTo(960);
        assertThat(response.midiNotes()).hasSize(1);
        assertThat(response.midiNotes().getFirst().pitch()).isEqualTo(60);
    }

    @Test
    void rejectsViewerBeforeCallingHtiService() {
        User owner = User.create("owner@example.com").withId(1L);
        User viewer = User.create("viewer@example.com").withId(2L);
        Project project = new Project(owner, "곡", null, 701L);
        ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
        ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
        HtiMidiTransformPort transformer = mock(HtiMidiTransformPort.class);
        when(projects.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 2L)).thenReturn(
                Optional.of(new ProjectMember(project, viewer, ProjectMemberRole.VIEWER)));

        assertThatThrownBy(() -> new HtiMidiTransformUseCase(projects, members, transformer).execute(
                new HtiMidiTransformRequest(10L, 2L, "recording.wav", "audio/wav", new byte[]{1})))
                .isInstanceOf(ProjectApplicationException.class)
                .extracting("errorCode").isEqualTo(ProjectErrorCode.PROJECT_ACCESS_DENIED);
        verifyNoInteractions(transformer);
    }
}
