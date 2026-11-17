package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import glitched.adlips.application.media.port.out.MediaContentStoragePort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.port.out.HtiMidiTransformPort;
import glitched.adlips.application.project.port.out.ProjectClipRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.usecase.AnalyzeAudioClipUseCase;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.media.MediaFileType;
import glitched.adlips.domain.project.AudioSoundKind;
import glitched.adlips.domain.project.ClipSourceType;
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
import org.springframework.test.util.ReflectionTestUtils;

class AnalyzeAudioClipUseCaseTest {
    private final ProjectClipRepositoryPort clips = mock(ProjectClipRepositoryPort.class);
    private final ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
    private final MediaFileRepositoryPort media = mock(MediaFileRepositoryPort.class);
    private final MediaContentStoragePort storage = mock(MediaContentStoragePort.class);
    private final HtiMidiTransformPort inference = mock(HtiMidiTransformPort.class);
    private final AnalyzeAudioClipUseCase useCase =
            new AnalyzeAudioClipUseCase(clips, members, media, storage, inference, TransactionRunner.direct(), "v1");

    @Test
    void storesConcurrentNotesAndReusesTheAnalysisWithoutRepeatingInference() {
        var clip = clip();
        when(media.findById(7L)).thenReturn(Optional.of(MediaFile.readyGenerated(
                1L, "/files/audio.wav", "audio.wav", "audio.wav", MediaFileType.AUDIO, "audio/wav", 3)));
        when(storage.get("audio.wav")).thenReturn(new byte[]{1, 2, 3});
        var notes = List.of(note(60), note(64), note(67));
        when(inference.analyze(any())).thenReturn(new HtiMidiTransformPort.AudioResult("basic-pitch-test", notes));
        var first = useCase.execute(10L, 5L, 1L, AudioSoundKind.INSTRUMENT, false);
        var cached = useCase.execute(10L, 5L, 1L, AudioSoundKind.INSTRUMENT, false);
        assertThat(cached).isEqualTo(first);
        assertThat(cached.midiNotes()).extracting(value -> value.pitch()).containsExactly(60, 64, 67);
        assertThat(clip.getMidiNotes()).isEmpty();
        assertThat(clip.getMediaFileId()).isEqualTo(7L);
        verify(inference, times(1)).analyze(any());
        useCase.execute(10L, 5L, 1L, AudioSoundKind.INSTRUMENT, true);
        verify(inference, times(2)).analyze(any());
    }

    @Test
    void rejectsAnotherProjectBeforeReadingAudio() {
        clip();
        assertThatThrownBy(() -> useCase.execute(11L, 5L, 1L, AudioSoundKind.HUMMING, false))
                .isInstanceOf(ProjectApplicationException.class);
        verifyNoInteractions(storage, inference);
    }

    private ProjectClip clip() {
        var user = User.create("audio@example.test").withId(1L);
        var project = new Project(user, "곡", null, 5L);
        ReflectionTestUtils.setField(project, "id", 10L);
        var track = new ProjectTrack(project, user, "악기", "piano", 0);
        var clip = ProjectClip.createAudio(project, track, user, ClipSourceType.FILE_UPLOAD,
                7L, null, 0, 960, 0, 1000, 0);
        when(clips.findClipByIdAndIsDeletedFalse(5L)).thenReturn(Optional.of(clip));
        when(clips.findClipForUpdate(5L)).thenReturn(Optional.of(clip));
        when(members.findByProjectIdAndUserId(10L, 1L)).thenReturn(
                Optional.of(new ProjectMember(project, user, ProjectMemberRole.OWNER)));
        return clip;
    }

    private HtiMidiTransformPort.Note note(int pitch) {
        return new HtiMidiTransformPort.Note(pitch, 0, 480, 100, null, Map.of());
    }
}
