package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.port.out.*;
import glitched.adlips.application.project.usecase.TrackInstrumentUpdateUseCase;
import glitched.adlips.domain.project.*;
import glitched.adlips.domain.user.User;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TrackInstrumentUpdateUseCaseTest {
    final User owner=User.create("owner@example.com").withId(1L);
    final Project project=new Project(owner,"곡",null,0L);
    final ProjectTrack track=new ProjectTrack(project,owner,"피아노","piano",0);
    final ProjectRepositoryPort projects=mock(ProjectRepositoryPort.class);
    final ProjectTrackRepositoryPort tracks=mock(ProjectTrackRepositoryPort.class);
    final ProjectClipRepositoryPort clips=mock(ProjectClipRepositoryPort.class);
    final EditorDraftRepositoryPort drafts=mock(EditorDraftRepositoryPort.class);
    final ProjectExportRepositoryPort exports=mock(ProjectExportRepositoryPort.class);
    final TrackInstrumentUpdateUseCase useCase=new TrackInstrumentUpdateUseCase(projects,tracks,clips,drafts,exports,TransactionRunner.direct());
    TrackInstrumentUpdateUseCaseTest(){
        when(projects.findByIdAndDeletedAtIsNullForUpdate(10L)).thenReturn(Optional.of(project));
        when(tracks.findTrackByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(track));
        when(tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(track));
    }
    @Test void ownTrackChangesToneWithoutChangingNotesAndQueuesPublishedMix(){
        var note=new MidiNote(60,0,480,90,null,Map.of());
        var clip=ProjectClip.createMidi(project,track,owner,0,480,List.of(note),0);
        when(clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(20L)).thenReturn(List.of(clip));
        project.startPublishing();project.publish(java.time.LocalDateTime.now());track.approve();track.replaceRenderedMedia(9L);
        useCase.execute(10L,20L,1L,"electric-piano");
        assertThat(track.getInstrument()).isEqualTo("electric-piano");
        assertThat(clip.getMidiNotes()).containsExactly(note);assertThat(track.getMediaFileId()).isNull();
        verify(exports).save(any(ProjectExport.class));
    }
    @ParameterizedTest
    @CsvSource({
        "electric-guitar, electric-guitar",
        "electric-guitar, distortion-guitar",
        "nylon-guitar, acoustic-guitar",
        "vocal, vocal",
        "vocal, backing-vocal",
        "backing-vocal, rap"
    })
    void allowsChangingToATeammatesInstrumentFamily(String existing, String selected){
        var other=new ProjectTrack(project,User.create("other@example.com").withId(2L),"팀원 레이어",existing,1);
        when(tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(track,other));

        assertThat(useCase.execute(10L,20L,1L,selected)).isEqualTo(selected);
        assertThat(track.getInstrument()).isEqualTo(selected);
        assertThat(other.getInstrument()).isEqualTo(existing);
    }
    @Test void sharingAGuitarPreservesSavedNotesAndPrivateDrafts(){
        var other=new ProjectTrack(project,User.create("other@example.com").withId(2L),"리듬 기타","electric-guitar",1);
        when(tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(track,other));
        var note=new MidiNote(60,0,480,90,null,Map.of());
        var clip=ProjectClip.createMidi(project,track,owner,0,480,List.of(note),0);
        when(clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(20L)).thenReturn(List.of(clip));
        var draftNote=new MidiNote(64,480,240,100,null,Map.of());
        var draft=EditorDraft.create(UUID.randomUUID(),1L,10L,20L,null,
                new EditorDraftSnapshot(0,720,List.of(draftNote)),null,UUID.randomUUID(),java.time.Instant.now());
        when(drafts.findActiveByProjectAndOwner(10L,1L)).thenReturn(List.of(draft));

        useCase.execute(10L,20L,1L,"electric-guitar");

        assertThat(clip.getMidiNotes()).containsExactly(note);
        assertThat(draft.content().midiNotes()).containsExactly(draftNote);
        assertThat(draft.status()).isEqualTo(EditorDraft.Status.ACTIVE);
        verify(drafts,never()).save(any());
    }
    @Test void preventsOtherUserAndPendingTrackAndCompletedProjectChanges(){
        assertThatThrownBy(()->useCase.execute(10L,20L,2L,"organ")).isInstanceOf(ProjectApplicationException.class);
        track.submitForReview();assertThatThrownBy(()->useCase.execute(10L,20L,1L,"organ")).isInstanceOf(ProjectApplicationException.class);
        track.approve();project.startPublishing();project.publish(java.time.LocalDateTime.now());project.complete();
        assertThatThrownBy(()->useCase.execute(10L,20L,1L,"organ")).isInstanceOf(ProjectApplicationException.class);
    }
    @Test void neverDropsOutOfRangeNotesWhenChangingToDrumsOrVocal(){
        var clip=ProjectClip.createMidi(project,track,owner,0,480,List.of(new MidiNote(96,0,480,90,null,Map.of())),0);
        when(clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(20L)).thenReturn(List.of(clip));
        assertThatThrownBy(()->useCase.execute(10L,20L,1L,"drum-kit")).isInstanceOf(ProjectApplicationException.class);
        assertThatThrownBy(()->useCase.execute(10L,20L,1L,"vocal")).isInstanceOf(ProjectApplicationException.class);
        assertThat(track.getInstrument()).isEqualTo("piano");assertThat(clip.getMidiNotes().getFirst().pitch()).isEqualTo(96);
    }
    @Test void audioOnlyLayerCanBeClassifiedAsVocal(){
        useCase.execute(10L,20L,1L,"vocal");assertThat(track.getInstrument()).isEqualTo("vocal");
    }
}
