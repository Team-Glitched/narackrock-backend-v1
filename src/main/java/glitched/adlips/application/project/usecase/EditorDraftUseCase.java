package glitched.adlips.application.project.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.EditorDraftSaveRequest;
import glitched.adlips.application.project.dto.request.MidiClipCreateRequest;
import glitched.adlips.application.project.dto.request.MidiClipSaveRequest;
import glitched.adlips.application.project.dto.request.MidiNoteRequest;
import glitched.adlips.application.project.port.out.*;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.project.*;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class EditorDraftUseCase {
    private final ProjectRepositoryPort projects;
    private final ProjectMemberRepositoryPort members;
    private final ProjectTrackRepositoryPort tracks;
    private final ProjectClipRepositoryPort clips;
    private final UserRepositoryPort users;
    private final EditorDraftRepositoryPort drafts;
    private final MidiClipCreateUseCase createClip;
    private final MidiClipSaveUseCase saveClip;
    private final TransactionRunner transactions;
    private final Clock clock;

    public EditorDraftUseCase(ProjectRepositoryPort projects, ProjectMemberRepositoryPort members,
                              ProjectTrackRepositoryPort tracks, ProjectClipRepositoryPort clips,
                              UserRepositoryPort users, EditorDraftRepositoryPort drafts,
                              MidiClipCreateUseCase createClip, MidiClipSaveUseCase saveClip,
                              TransactionRunner transactions, Clock clock) {
        this.projects = projects; this.members = members; this.tracks = tracks; this.clips = clips;
        this.users = users; this.drafts = drafts; this.createClip = createClip; this.saveClip = saveClip;
        this.transactions = transactions; this.clock = clock;
    }

    public List<EditorDraft> list(long projectId, long userId) {
        return transactions.readOnly(() -> {
            access(projectId, userId, false);
            return drafts.findActiveByProjectAndOwner(projectId, userId);
        });
    }

    public EditorDraft get(long projectId, UUID id, long userId) {
        return transactions.readOnly(() -> {
            access(projectId, userId, false);
            return owned(projectId, id, userId);
        });
    }

    public EditorDraft save(long projectId, long trackId, UUID id, long userId, EditorDraftSaveRequest request) {
        return transactions.required(() -> translate(() -> {
            if (request == null || request.content() == null || request.mutationId() == null
                    || request.expectedRevision() < 0) throw invalid();
            var project = access(projectId, userId, true);
            validateTrack(project, trackId, userId);
            request.content().validateMaxTick(project.getMaxTick());
            var existing = drafts.findById(id).orElse(null);
            if (existing != null) {
                requireOwner(existing, projectId, userId);
                if (existing.trackId() != trackId || !Objects.equals(existing.targetClipId(), request.targetClipId())
                        || !Objects.equals(existing.baseClip(), request.baseClip())) throw conflict();
                if (request.targetClipId() != null) validateClip(projectId, trackId, request.targetClipId(), userId);
                return drafts.save(existing.update(request.expectedRevision(), request.mutationId(),
                        request.content(), clock.instant()));
            }
            if (request.expectedRevision() != 0) throw conflict();
            // The project write lock serializes both first-save and subsequent writes, including other devices.
            boolean duplicate = drafts.findActiveByProjectAndOwner(projectId, userId).stream()
                    .anyMatch(d -> d.trackId() == trackId && Objects.equals(d.targetClipId(), request.targetClipId()));
            if (duplicate) throw conflict();
            if (request.targetClipId() != null) {
                var clip = validateClip(projectId, trackId, request.targetClipId(), userId);
                if (!EditorDraftSnapshot.from(clip).equals(request.baseClip())) throw baseChanged();
            } else if (request.baseClip() != null) throw invalid();
            return drafts.save(EditorDraft.create(id, userId, projectId, trackId, request.targetClipId(),
                    request.content(), request.baseClip(), request.mutationId(), clock.instant()));
        }));
    }

    public EditorDraft commit(long projectId, UUID id, long userId, long expectedRevision) {
        return transactions.required(() -> translate(() -> {
            var project = access(projectId, userId, true);
            var draft = owned(projectId, id, userId);
            // Keep a receipt so a lost HTTP response cannot create the same new clip twice.
            if (draft.status() == EditorDraft.Status.COMMITTED
                    && (expectedRevision == draft.revision() - 1 || expectedRevision == draft.revision())) return draft;
            draft.requireRevision(expectedRevision);
            validateTrack(project, draft.trackId(), userId);
            draft.content().validateMaxTick(project.getMaxTick());
            // Empty drafts keep their start with a placeholder capped to the remaining timeline;
            // old drafts left beyond a slower BPM's timeline fall back to the song start.
            var content = draft.content();
            if (content.midiNotes().isEmpty()) {
                long placeholder = Math.max(1, (project.getPpq() + 3) / 4);
                long remainingTicks = project.getMaxTick() - content.startTick();
                content = remainingTicks > 0
                        ? new EditorDraftSnapshot(content.startTick(), Math.min(placeholder, remainingTicks), List.of())
                        : new EditorDraftSnapshot(0, placeholder, List.of());
            }
            var notes = content.midiNotes().stream().map(n -> new MidiNoteRequest(n.pitch(), n.startTick(),
                    n.durationTick(), n.velocity(), n.effectType(), n.effectParams())).toList();
            Long clipId = draft.targetClipId();
            if (clipId == null) {
                clipId = createClip.execute(new MidiClipCreateRequest(projectId, draft.trackId(), userId,
                        content.startTick(), content.durationTick(), notes)).clipId();
            } else {
                var clip = validateClip(projectId, draft.trackId(), clipId, userId);
                if (!EditorDraftSnapshot.from(clip).equals(draft.baseClip())) throw baseChanged();
                saveClip.execute(new MidiClipSaveRequest(clipId, userId, content.startTick(), content.durationTick(), notes));
            }
            return drafts.save(draft.commit(expectedRevision, clipId, clock.instant()));
        }));
    }

    public EditorDraft discard(long projectId, UUID id, long userId, long expectedRevision) {
        return transactions.required(() -> translate(() -> {
            access(projectId, userId, true);
            var draft = owned(projectId, id, userId);
            if (draft.status() == EditorDraft.Status.DISCARDED && expectedRevision == draft.revision() - 1) return draft;
            return drafts.save(draft.discard(expectedRevision, clock.instant()));
        }));
    }

    private Project access(long projectId, long userId, boolean write) {
        var project = (write ? projects.findByIdAndDeletedAtIsNullForUpdate(projectId)
                : projects.findByIdAndDeletedAtIsNull(projectId)).orElseThrow(() ->
                new ProjectApplicationException(ProjectErrorCode.PROJECT_NOT_FOUND, "프로젝트를 찾을 수 없습니다."));
        users.findById(userId).filter(u -> u.isActive()).orElseThrow(() -> denied());
        members.findByProjectIdAndUserId(projectId, userId)
                .filter(m -> m.getRole() == ProjectMemberRole.OWNER || m.getRole() == ProjectMemberRole.EDITOR)
                .orElseThrow(() -> denied());
        if (write && project.getStatus() == ProjectStatus.COMPLETED) throw denied();
        return project;
    }

    private void validateTrack(Project project, long trackId, long userId) {
        var track = tracks.findTrackByIdAndIsDeletedFalse(trackId).orElseThrow(() ->
                new ProjectApplicationException(ProjectErrorCode.TRACK_NOT_FOUND, "레이어를 찾을 수 없습니다."));
        if (!track.belongsTo(project) || !track.getOwner().getId().equals(userId)) throw denied();
        if (track.getInstrument() == null || track.getInstrument().isBlank() || InstrumentFamily.audioOnly(track.getInstrument())
                || (track.getApprovalStatus() != ApprovalStatus.DRAFT
                    && track.getApprovalStatus() != ApprovalStatus.APPROVED)) throw denied();
    }

    private ProjectClip validateClip(long projectId, long trackId, long clipId, long userId) {
        var clip = clips.findClipByIdAndIsDeletedFalse(clipId).orElseThrow(() ->
                new ProjectApplicationException(ProjectErrorCode.CLIP_NOT_FOUND, "클립을 찾을 수 없습니다."));
        if (!clip.getOwner().getId().equals(userId) || !clip.getProject().getId().equals(projectId)
                || !clip.getTrack().getId().equals(trackId) || clip.getClipType() != ClipType.MIDI
                || (clip.getApprovalStatus() != ApprovalStatus.DRAFT && clip.getApprovalStatus() != ApprovalStatus.APPROVED)) {
            throw denied();
        }
        return clip;
    }

    private EditorDraft owned(long projectId, UUID id, long userId) {
        var draft = drafts.findById(id).orElseThrow(EditorDraftUseCase::notFound);
        requireOwner(draft, projectId, userId);
        return draft;
    }

    private void requireOwner(EditorDraft draft, long projectId, long userId) {
        if (draft.projectId() != projectId || draft.ownerId() != userId) throw notFound();
    }

    private <T> T translate(Supplier<T> action) {
        try { return action.get(); }
        catch (IllegalStateException e) { throw conflict(); }
        catch (IllegalArgumentException | NullPointerException e) { throw invalid(); }
    }

    private static ProjectApplicationException denied() {
        return new ProjectApplicationException(ProjectErrorCode.PROJECT_ACCESS_DENIED, "본인의 편집 가능한 레이어만 작업할 수 있습니다.");
    }
    private static ProjectApplicationException notFound() {
        return new ProjectApplicationException(ProjectErrorCode.EDITOR_DRAFT_NOT_FOUND, "개인 작업 초안을 찾을 수 없습니다.");
    }
    private static ProjectApplicationException conflict() {
        return new ProjectApplicationException(ProjectErrorCode.EDITOR_DRAFT_CONFLICT, "다른 탭이나 기기에서 작업이 변경되었습니다. 복구본을 확인해 주세요.");
    }
    private static ProjectApplicationException baseChanged() {
        return new ProjectApplicationException(ProjectErrorCode.EDITOR_DRAFT_BASE_CHANGED, "원본 클립이 변경되었습니다. 초안을 보관하고 원본을 다시 확인해 주세요.");
    }
    private static ProjectApplicationException invalid() {
        return new ProjectApplicationException(ProjectErrorCode.INVALID_MIDI_NOTE_DATA, "임시 저장할 MIDI 데이터를 확인해 주세요.");
    }
}
