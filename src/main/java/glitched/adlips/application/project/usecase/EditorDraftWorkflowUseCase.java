package glitched.adlips.application.project.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.EditorDraftBatchRequest;
import glitched.adlips.application.project.dto.request.ProjectContributionCreateRequest;
import glitched.adlips.application.project.dto.response.ProjectContributionCreateResponse;
import glitched.adlips.application.project.port.out.*;
import glitched.adlips.domain.project.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class EditorDraftWorkflowUseCase {
    private final EditorDraftUseCase drafts;
    private final ProjectRepositoryPort projects;
    private final ProjectTrackRepositoryPort tracks;
    private final ProjectClipRepositoryPort clips;
    private final ProjectContributionCreateUseCase contributions;
    private final TransactionRunner transactions;

    public EditorDraftWorkflowUseCase(EditorDraftUseCase drafts, ProjectRepositoryPort projects,
                                      ProjectTrackRepositoryPort tracks, ProjectClipRepositoryPort clips,
                                      ProjectContributionCreateUseCase contributions, TransactionRunner transactions) {
        this.drafts = drafts; this.projects = projects; this.tracks = tracks; this.clips = clips;
        this.contributions = contributions; this.transactions = transactions;
    }

    public List<EditorDraft> commitAll(long projectId, long userId, EditorDraftBatchRequest request) {
        return transactions.required(() -> commitSelected(projectId, userId, request));
    }

    public ProjectContributionCreateResponse submit(long projectId, long userId, EditorDraftBatchRequest request) {
        // Materializing the working copies and marking contribution targets PENDING must either both succeed or both roll back.
        return transactions.required(() -> {
            commitSelected(projectId, userId, request);
            var items = new ArrayList<ProjectContributionCreateRequest.Item>();
            for (var track : tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(projectId)) {
                if (!track.getOwner().getId().equals(userId)) continue;
                var ownedClips = clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(track.getId()).stream()
                        .filter(c -> c.getOwner().getId().equals(userId) && c.getApprovalStatus() == ApprovalStatus.DRAFT)
                        .toList();
                if (!ownedClips.isEmpty() && track.getApprovalStatus() == ApprovalStatus.DRAFT) {
                    items.add(new ProjectContributionCreateRequest.Item(
                            ContributionChangeType.ADD, ContributionTargetType.TRACK, track.getId()));
                }
                ownedClips.forEach(c -> items.add(new ProjectContributionCreateRequest.Item(
                        ContributionChangeType.ADD, ContributionTargetType.CLIP, c.getId())));
            }
            return contributions.execute(new ProjectContributionCreateRequest(
                    projectId, userId, request.description(), request.baseProjectVersion(), items));
        });
    }

    private List<EditorDraft> commitSelected(long projectId, long userId, EditorDraftBatchRequest request) {
        if (request == null || request.drafts() == null) throw conflict();
        // Same lock as autosave prevents a new save between checking revisions and submission.
        projects.findByIdAndDeletedAtIsNullForUpdate(projectId);
        var active = drafts.list(projectId, userId);
        var ids = new HashSet<java.util.UUID>();
        for (var ref : request.drafts()) {
            if (ref == null || ref.draftId() == null || ref.expectedRevision() < 1 || !ids.add(ref.draftId())) throw conflict();
        }
        for (var draft : active) {
            if (request.drafts().stream().noneMatch(r -> r.draftId().equals(draft.draftId())
                    && r.expectedRevision() == draft.revision())) throw conflict();
        }
        return request.drafts().stream()
                .map(ref -> drafts.commit(projectId, ref.draftId(), userId, ref.expectedRevision())).toList();
    }

    private ProjectApplicationException conflict() {
        return new ProjectApplicationException(ProjectErrorCode.EDITOR_DRAFT_CONFLICT,
                "초안이 변경되었습니다. 최신 작업을 확인한 뒤 다시 시도해 주세요.");
    }
}
