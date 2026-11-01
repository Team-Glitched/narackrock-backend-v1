package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.dto.request.EditorDraftBatchRequest;
import glitched.adlips.application.project.dto.request.ProjectContributionCreateRequest;
import glitched.adlips.application.project.port.out.*;
import glitched.adlips.application.project.usecase.*;
import glitched.adlips.domain.project.*;
import glitched.adlips.domain.user.User;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class EditorDraftWorkflowUseCaseTest {
    final EditorDraftUseCase drafts = mock(EditorDraftUseCase.class);
    final ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
    final ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);
    final ProjectClipRepositoryPort clips = mock(ProjectClipRepositoryPort.class);
    final ProjectContributionCreateUseCase contributions = mock(ProjectContributionCreateUseCase.class);
    final EditorDraftWorkflowUseCase workflow = new EditorDraftWorkflowUseCase(
            drafts, projects, tracks, clips, contributions, TransactionRunner.direct());
    final EditorDraft draft = EditorDraft.create(UUID.randomUUID(), 1, 10, 20, null,
            new EditorDraftSnapshot(0, 480, List.of()), null, UUID.randomUUID(), Instant.now());

    @Test void rejectsSubmissionIfAnotherDeviceAddedAnUnseenDraft() {
        when(drafts.list(10, 1)).thenReturn(List.of(draft));
        assertThatThrownBy(() -> workflow.submit(10, 1, new EditorDraftBatchRequest(List.of(), "작업", null)))
                .extracting("errorCode").isEqualTo(ProjectErrorCode.EDITOR_DRAFT_CONFLICT);
        verifyNoInteractions(contributions);
    }

    @Test void commitsExactDraftRevisionsBeforeCollectingSubmissionItems() {
        when(drafts.list(10, 1)).thenReturn(List.of(draft));
        when(drafts.commit(10, draft.draftId(), 1, 1)).thenReturn(draft.commit(1, 90, Instant.now()));
        var user = User.create("draft@test.com").withId(1L);
        var project = new Project(user, "곡", null, 0L);
        var track = new ProjectTrack(project, user, "피아노", "piano", 0);
        ReflectionTestUtils.setField(track, "id", 20L);
        var clip = ProjectClip.createMidi(project, track, user, 0, 480, List.of(), 0);
        ReflectionTestUtils.setField(clip, "id", 90L);
        when(tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(track));
        when(clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(20L)).thenReturn(List.of(clip));
        workflow.submit(10, 1, new EditorDraftBatchRequest(
                List.of(new EditorDraftBatchRequest.Reference(draft.draftId(), 1)), "멜로디", null));
        var order = inOrder(drafts, tracks, contributions);
        order.verify(drafts).list(10, 1);
        order.verify(drafts).commit(10, draft.draftId(), 1, 1);
        order.verify(tracks).findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(10L);
        var argument = ArgumentCaptor.forClass(ProjectContributionCreateRequest.class);
        verify(contributions).execute(argument.capture());
        assertThat(argument.getValue().items()).extracting(ProjectContributionCreateRequest.Item::targetId)
                .containsExactly(20L, 90L);
    }
}
