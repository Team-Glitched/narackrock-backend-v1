package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.port.out.*;
import glitched.adlips.application.project.usecase.*;
import glitched.adlips.application.project.dto.request.EditorDraftSaveRequest;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.project.*;
import glitched.adlips.domain.user.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EditorDraftUseCaseTest {
    final ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
    final ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
    final ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);
    final ProjectClipRepositoryPort clips = mock(ProjectClipRepositoryPort.class);
    final UserRepositoryPort users = mock(UserRepositoryPort.class);
    final EditorDraftRepositoryPort drafts = mock(EditorDraftRepositoryPort.class);
    final Map<UUID, EditorDraft> stored = new HashMap<>();
    final User user = User.create("draft@test.com").withId(1L);
    final Project project = new Project(user, "곡", null, 0L);
    final ProjectTrack track = new ProjectTrack(project, user, "피아노", "piano", 0);
    final EditorDraftSnapshot content = new EditorDraftSnapshot(0, 480,
            List.of(new MidiNote(60, 0, 480, 100, null, Map.of())));
    final Clock clock = Clock.fixed(Instant.parse("2026-10-28T00:00:00Z"), ZoneOffset.UTC);
    EditorDraftUseCase useCase;

    @BeforeEach void setUp() {
        ReflectionTestUtils.setField(project, "id", 10L);
        ReflectionTestUtils.setField(track, "id", 20L);
        when(projects.findByIdAndDeletedAtIsNullForUpdate(10L)).thenReturn(Optional.of(project));
        when(projects.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 1L)).thenReturn(Optional.of(
                new ProjectMember(project, user, ProjectMemberRole.OWNER)));
        when(tracks.findTrackByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(track));
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(drafts.findById(any())).thenAnswer(call -> Optional.ofNullable(stored.get(call.getArgument(0))));
        when(drafts.findActiveByProjectAndOwner(10L, 1L)).thenAnswer(call -> stored.values().stream()
                .filter(d -> d.status() == EditorDraft.Status.ACTIVE).toList());
        when(drafts.save(any())).thenAnswer(call -> {
            EditorDraft value = call.getArgument(0); stored.put(value.draftId(), value); return value;
        });
        when(clips.save(any(ProjectClip.class))).thenAnswer(call -> {
            ProjectClip value = call.getArgument(0); ReflectionTestUtils.setField(value, "id", 90L); return value;
        });
        useCase = new EditorDraftUseCase(projects, members, tracks, clips, users, drafts,
                new MidiClipCreateUseCase(projects, members, tracks, clips, users),
                new MidiClipSaveUseCase(clips), TransactionRunner.direct(), clock);
    }

    EditorDraft save(UUID id, long revision, Long clipId, EditorDraftSnapshot base) {
        return useCase.save(10L, 20L, id, 1L,
                new EditorDraftSaveRequest(revision, UUID.randomUUID(), clipId, content, base));
    }

    @Test void oldEmptyDraftCanBeSavedAndCommittedAfterTempoChange() {
        var id = UUID.randomUUID();
        var empty = new EditorDraftSnapshot(40_000, 1000, List.of());
        useCase.save(10L, 20L, id, 1L,
                new EditorDraftSaveRequest(0, UUID.randomUUID(), null, empty, null));
        project.updateTiming(60, 4, 4);
        useCase.save(10L, 20L, id, 1L,
                new EditorDraftSaveRequest(1, UUID.randomUUID(), null, empty, null));
        useCase.commit(10L, id, 1L, 2);
        verify(clips).save(argThat(clip -> clip.getStartTick() == 0
                && clip.getDurationTick() == 120 && clip.getMidiNotes().isEmpty()));
    }

    @Test void committingEmptyContentPreservesItsPositionWhenWithinTheTimeline() {
        var id = UUID.randomUUID();
        useCase.save(10L, 20L, id, 1L, new EditorDraftSaveRequest(0, UUID.randomUUID(), null,
                new EditorDraftSnapshot(960, 10_000, List.of()), null));
        useCase.commit(10L, id, 1L, 1);
        verify(clips).save(argThat(clip -> clip.getStartTick() == 960
                && clip.getDurationTick() == 120 && clip.getMidiNotes().isEmpty()));
    }

    @Test void autosaveDoesNotCreateClipsOrIncrementTheProjectVersion() {
        int minor = project.getMinorVersion();
        var result = save(UUID.randomUUID(), 0, null, null);
        assertThat(result.revision()).isEqualTo(1);
        assertThat(project.getMinorVersion()).isEqualTo(minor);
        verifyNoInteractions(clips);
    }

    @Test void approvedClipIsUnchangedUntilExplicitCommit() {
        var clip = ProjectClip.createMidi(project, track, user, 0, 480, List.of(), 0);
        clip.approve();
        when(clips.findClipByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(clip));
        var result = save(UUID.randomUUID(), 0, 30L, EditorDraftSnapshot.from(clip));
        assertThat(clip.getMidiNotes()).isEmpty();
        useCase.commit(10L, result.draftId(), 1L, 1);
        assertThat(clip.getMidiNotes()).isEqualTo(content.midiNotes());
    }

    @Test void rejectsAnotherUsersDraftEvenWhenTheUuidIsKnown() {
        var result = save(UUID.randomUUID(), 0, null, null);
        assertThatThrownBy(() -> useCase.get(10L, result.draftId(), 2L))
                .isInstanceOf(ProjectApplicationException.class);
    }

    @Test void rejectsEditingAnotherUsersTrack() {
        ReflectionTestUtils.setField(track, "owner", User.create("other@test.com").withId(2L));
        assertThatThrownBy(() -> save(UUID.randomUUID(), 0, null, null))
                .isInstanceOf(ProjectApplicationException.class);
        verify(drafts, never()).save(any());
    }

    @Test void rejectsDuplicateActiveDraftForTheSameTarget() {
        save(UUID.randomUUID(), 0, null, null);
        assertThatThrownBy(() -> save(UUID.randomUUID(), 0, null, null))
                .extracting("errorCode").isEqualTo(ProjectErrorCode.EDITOR_DRAFT_CONFLICT);
    }

    @Test void staleRevisionCannotOverwriteOrCommitTheDraft() {
        var id = UUID.randomUUID(); save(id, 0, null, null); save(id, 1, null, null);
        assertThatThrownBy(() -> save(id, 1, null, null)).isInstanceOf(ProjectApplicationException.class);
        assertThatThrownBy(() -> useCase.commit(10L, id, 1L, 1)).isInstanceOf(ProjectApplicationException.class);
        verify(clips, never()).save(any());
    }

    @Test void commitIsIdempotentAndLateAutosavesCannotResurrectIt() {
        var id = UUID.randomUUID(); save(id, 0, null, null);
        assertThat(useCase.commit(10L, id, 1L, 1).committedClipId()).isEqualTo(90L);
        assertThat(useCase.commit(10L, id, 1L, 1).committedClipId()).isEqualTo(90L);
        verify(clips, times(1)).save(any());
        assertThatThrownBy(() -> save(id, 1, null, null)).isInstanceOf(ProjectApplicationException.class);
    }

    @Test void rejectsCommitIfOriginalClipChangedAfterEditingStarted() {
        var clip = ProjectClip.createMidi(project, track, user, 0, 480, List.of(), 0);
        when(clips.findClipByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(clip));
        var result = save(UUID.randomUUID(), 0, 30L, EditorDraftSnapshot.from(clip));
        clip.updateMidi(0, 960, List.of());
        assertThatThrownBy(() -> useCase.commit(10L, result.draftId(), 1L, 1))
                .extracting("errorCode").isEqualTo(ProjectErrorCode.EDITOR_DRAFT_BASE_CHANGED);
    }

    @Test void submittedClipCannotBeAutosavedOrCommitted() {
        var clip = ProjectClip.createMidi(project, track, user, 0, 480, List.of(), 0);
        when(clips.findClipByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(clip));
        var base = EditorDraftSnapshot.from(clip);
        var result = save(UUID.randomUUID(), 0, 30L, base);
        clip.submitForReview();
        assertThatThrownBy(() -> useCase.commit(10L, result.draftId(), 1L, 1))
                .isInstanceOf(ProjectApplicationException.class);
    }
}
