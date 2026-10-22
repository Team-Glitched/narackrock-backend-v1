package glitched.adlips.application.shorts;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.port.out.ProjectClipRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.shorts.port.out.ShortsCompositionQueryPort;
import glitched.adlips.application.shorts.port.out.ShortsCompositionQueryItem;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.project.*;
import glitched.adlips.domain.user.User;
import java.util.*;
import org.junit.jupiter.api.Test;

class GetShortsParticipantTracksUseCaseTest {
    final ShortsCompositionQueryPort shorts = mock(ShortsCompositionQueryPort.class);
    final ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
    final ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);
    final ProjectClipRepositoryPort clips = mock(ProjectClipRepositoryPort.class);
    final MediaFileRepositoryPort media = mock(MediaFileRepositoryPort.class);

    GetShortsParticipantTracksUseCase useCase() {
        return new GetShortsParticipantTracksUseCase(shorts, projects, tracks, clips, media, TransactionRunner.direct());
    }

    @Test
    void readsApprovedAudioAndMidiWithoutJoiningTheProjectEvenWhenCompleted() {
        var owner = User.create("owner@test.com").withId(1L);
        var project = new Project(owner, "곡", "설명", 0L);
        project.startPublishing(); project.publish(java.time.LocalDateTime.now()); project.complete();
        when(shorts.findByShortId(10L)).thenReturn(Optional.of(new ShortsCompositionQueryItem(
                10L, 20L, "곡", ProjectStatus.COMPLETED, null, 1L, "mix.wav", 2L, "layers.zip")));
        when(projects.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(project));
        var track = mock(ProjectTrack.class);
        when(track.getId()).thenReturn(30L); when(track.getOwner()).thenReturn(owner);
        when(track.getInstrument()).thenReturn("piano"); when(track.getVolume()).thenReturn(75);
        when(track.getApprovalStatus()).thenReturn(ApprovalStatus.APPROVED);
        when(tracks.findByProjectIdAndApprovalStatusAndIsDeletedFalse(20L, ApprovalStatus.APPROVED))
                .thenReturn(List.of(track));
        var midi = ProjectClip.createMidi(project, new ProjectTrack(project, owner, "피아노", "piano", 0),
                owner, 960, 480, List.of(new MidiNote(60, 0, 480, 100, null, null)), 0);
        var audio = ProjectClip.createAudio(project, new ProjectTrack(project, owner, "오디오", null, 1),
                owner, ClipSourceType.FILE_UPLOAD, 5L, null, 0, 480, 250, 500, 0);
        when(clips.findByTrackIdAndApprovalStatusAndIsDeletedFalseOrderByStartTickAscIdAsc(30L, ApprovalStatus.APPROVED))
                .thenReturn(List.of(midi, audio));

        var result = useCase().execute(10L);

        assertThat(result.project().bpm()).isEqualTo(120);
        assertThat(result.tracks()).hasSize(1);
        assertThat(result.tracks().getFirst().ownerId()).isEqualTo(1L);
        assertThat(result.tracks().getFirst().volume()).isEqualTo(75);
        assertThat(result.tracks().getFirst().clips().getFirst().midiNotes()).hasSize(1);
        assertThat(result.tracks().getFirst().clips().get(1).clipOffsetMs()).isEqualTo(250);
        verify(tracks, never()).findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(any());
        verify(clips, never()).findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(any());
    }

    @Test
    void rejectsMissingOrUnlinkedShorts() {
        assertThatThrownBy(() -> useCase().execute(99L)).isInstanceOf(ShortsComposersApplicationException.class);
        verifyNoInteractions(projects, tracks, clips, media);
        when(shorts.findByShortId(10L)).thenReturn(Optional.of(new ShortsCompositionQueryItem(
                10L, null, null, null, null, 1L, "mix.wav", null, null)));
        assertThatThrownBy(() -> useCase().execute(10L)).isInstanceOf(ShortsComposersApplicationException.class);
    }
}
