package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.port.out.*;
import glitched.adlips.application.project.usecase.ProjectTimingUpdateUseCase;
import glitched.adlips.domain.project.*;
import glitched.adlips.domain.user.User;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class ProjectTimingUpdateUseCaseTest {
    final User owner = User.create("timing-owner@example.com").withId(1L);
    final Project project = new Project(owner, "Timing", null, 0L);
    final ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
    final ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
    final ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);
    final ProjectClipRepositoryPort clips = mock(ProjectClipRepositoryPort.class);
    final EditorDraftRepositoryPort drafts = mock(EditorDraftRepositoryPort.class);
    final ProjectExportRepositoryPort exports = mock(ProjectExportRepositoryPort.class);
    final ProjectTimingUpdateUseCase timing = new ProjectTimingUpdateUseCase(projects, members, tracks, clips, drafts, exports, TransactionRunner.direct());

    ProjectTimingUpdateUseCaseTest() {
        when(projects.findByIdAndDeletedAtIsNullForUpdate(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L,1L)).thenReturn(Optional.of(new ProjectMember(project,owner,ProjectMemberRole.OWNER)));
        when(tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of());
        when(drafts.findActiveByProject(10L)).thenReturn(List.of());
    }
    @Test void ownerUpdatesSharedTimingAndVersion() {
        var result=timing.execute(10L,1L,90,6,8);
        assertThat(result.bpm()).isEqualTo(90);
        assertThat(result.timeSignatureNumerator()).isEqualTo(6);
        assertThat(result.timeSignatureDenominator()).isEqualTo(8);
        assertThat(result.maxTick()).isEqualTo(43_200);
        assertThat(project.getMinorVersion()).isEqualTo(2);
        timing.execute(10L,1L,90,6,8);
        assertThat(project.getMinorVersion()).isEqualTo(2);
        verifyNoInteractions(exports);
    }
    @Test void contributorCannotChangeSharedTiming() {
        when(members.findByProjectIdAndUserId(10L,2L)).thenReturn(Optional.of(new ProjectMember(project,User.create("editor@example.com").withId(2L),ProjectMemberRole.EDITOR)));
        assertThatThrownBy(()->timing.execute(10L,2L,90,3,4)).isInstanceOf(ProjectApplicationException.class).extracting("errorCode").isEqualTo(ProjectErrorCode.PROJECT_ACCESS_DENIED);
        assertThat(project.getBpm()).isEqualTo(120);
    }
    @Test void invalidTimingAndCompletedProjectsAreRejected() {
        for(int[] values:List.of(new int[]{29,4,4},new int[]{301,4,4},new int[]{120,0,4},new int[]{120,13,4},new int[]{120,4,3})) {
            assertThatThrownBy(()->timing.execute(10L,1L,values[0],values[1],values[2])).isInstanceOf(ProjectApplicationException.class);
        }
        project.startPublishing();project.publish(java.time.LocalDateTime.now());project.complete();
        assertThatThrownBy(()->timing.execute(10L,1L,100,4,4)).isInstanceOf(ProjectApplicationException.class);
    }
    @Test void tempoPreservesMidiTicksAndAudioSecondsAndInvalidatesRenderedTrack() {
        var track=new ProjectTrack(project,owner,"Piano","piano",0);
        var midi=ProjectClip.createMidi(project,track,owner,480,960,List.of(new MidiNote(60,0,480,91,null,Map.of())),0);
        var audio=ProjectClip.createAudio(project,track,owner,ClipSourceType.FILE_UPLOAD,21L,null,960,2880,100,3000,1);
        track.replaceRenderedMedia(9L);
        when(tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(track));
        when(clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(null)).thenReturn(List.of(midi,audio));
        timing.execute(10L,1L,60,3,4);
        assertThat(midi.getStartTick()).isEqualTo(480);assertThat(midi.getDurationTick()).isEqualTo(960);
        assertThat(midi.getStartTimeMs()).isEqualTo(1000);assertThat(midi.getDurationMs()).isEqualTo(2000);
        assertThat(midi.getMidiNotes().getFirst().velocity()).isEqualTo(91);
        assertThat(audio.getStartTimeMs()).isEqualTo(2000);assertThat(audio.getDurationMs()).isEqualTo(3000);
        assertThat(audio.getDurationTick()).isEqualTo(1440);assertThat(audio.getClipOffsetMs()).isEqualTo(100);
        assertThat(track.getMediaFileId()).isNull();
    }
    @Test void slowingTempoCannotTruncateSavedClipsOrPrivateDrafts() {
        var track=new ProjectTrack(project,owner,"Piano","piano",0);
        var clip=ProjectClip.createMidi(project,track,owner,40_000,1000,List.of(),0);
        when(tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(track));
        when(clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(null)).thenReturn(List.of(clip));
        assertThatThrownBy(()->timing.execute(10L,1L,60,4,4)).isInstanceOf(ProjectApplicationException.class);
        assertThat(project.getBpm()).isEqualTo(120);
        when(tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of());
        var draft=EditorDraft.create(UUID.randomUUID(),2L,10L,7L,null,new EditorDraftSnapshot(0,40_000,List.of()),null,UUID.randomUUID(),Instant.now());
        when(drafts.findActiveByProject(10L)).thenReturn(List.of(draft));
        assertThatThrownBy(()->timing.execute(10L,1L,60,4,4)).isInstanceOf(ProjectApplicationException.class);
        assertThat(project.getBpm()).isEqualTo(120);
    }
    @Test void publishedSongQueuesUpdatedMix() {
        project.startPublishing();project.publish(java.time.LocalDateTime.now());
        timing.execute(10L,1L,100,3,4);
        verify(exports).save(any(ProjectExport.class));
    }
}
