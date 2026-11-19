package glitched.adlips.application.project;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.port.out.*;
import glitched.adlips.application.project.usecase.EmptyTrackDeleteUseCase;
import glitched.adlips.domain.project.*;
import glitched.adlips.domain.user.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

class EmptyTrackDeleteUseCaseTest {
    private final ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
    private final ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);
    private final ProjectClipRepositoryPort clips = mock(ProjectClipRepositoryPort.class);
    private final EditorDraftRepositoryPort drafts = mock(EditorDraftRepositoryPort.class);
    private final ProjectContributionItemRepositoryPort contributionItems = mock(ProjectContributionItemRepositoryPort.class);
    private final User owner = User.create("owner@example.com").withId(1L);
    private final ProjectTrack track = mock(ProjectTrack.class);

    EmptyTrackDeleteUseCaseTest() {
        Project project = new Project(owner, "곡", "설명", 701L);
        when(projects.findByIdAndDeletedAtIsNullForUpdate(10L)).thenReturn(Optional.of(project));
        when(tracks.findTrackByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(track));
        when(track.belongsTo(project)).thenReturn(true);
        when(track.getOwner()).thenReturn(owner);
        when(track.getApprovalStatus()).thenReturn(ApprovalStatus.DRAFT);
    }

    @Test
    void preservesTrackWhenAnotherRequestAlreadySavedAClip() {
        when(clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(20L))
                .thenReturn(List.of(mock(ProjectClip.class)));

        useCase().execute(10L, 20L, 1L);

        verify(track, never()).delete();
    }

    @Test
    void preservesTrackWithAnActiveDraft() {
        EditorDraft draft = mock(EditorDraft.class);
        when(draft.trackId()).thenReturn(20L);
        when(drafts.findActiveByProjectAndOwner(10L, 1L)).thenReturn(List.of(draft));

        useCase().execute(10L, 20L, 1L);

        verify(track, never()).delete();
    }

    @Test
    void locksProjectBeforeCheckingAndRemovingEmptyTrack() {
        useCase().execute(10L, 20L, 1L);

        var order = inOrder(projects, tracks, clips, drafts, track);
        order.verify(projects).findByIdAndDeletedAtIsNullForUpdate(10L);
        order.verify(tracks).findTrackByIdAndIsDeletedFalse(20L);
        order.verify(clips).findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(20L);
        order.verify(drafts).findActiveByProjectAndOwner(10L, 1L);
        order.verify(track).delete();
    }

    private EmptyTrackDeleteUseCase useCase() {
        return new EmptyTrackDeleteUseCase(projects, tracks, clips, drafts, contributionItems, TransactionRunner.direct());
    }
}
