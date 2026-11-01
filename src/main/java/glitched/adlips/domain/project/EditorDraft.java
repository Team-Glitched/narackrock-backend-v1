package glitched.adlips.domain.project;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A private working copy. It never mutates a project, track, or clip. */
public record EditorDraft(UUID draftId, long ownerId, long projectId, long trackId, Long targetClipId,
                          EditorDraftSnapshot content, EditorDraftSnapshot baseClip,
                          long revision, UUID mutationId, Instant updatedAt,
                          Status status, Long committedClipId) {
    public enum Status { ACTIVE, COMMITTED, DISCARDED }

    public EditorDraft {
        Objects.requireNonNull(draftId);
        Objects.requireNonNull(content);
        Objects.requireNonNull(mutationId);
        Objects.requireNonNull(updatedAt);
        Objects.requireNonNull(status);
    }

    public static EditorDraft create(UUID id, long ownerId, long projectId, long trackId, Long targetClipId,
                                     EditorDraftSnapshot content, EditorDraftSnapshot baseClip,
                                     UUID mutationId, Instant now) {
        return new EditorDraft(id, ownerId, projectId, trackId, targetClipId, content, baseClip,
                1, mutationId, now, Status.ACTIVE, null);
    }

    public EditorDraft update(long expectedRevision, UUID mutation, EditorDraftSnapshot snapshot, Instant now) {
        if (status != Status.ACTIVE) throw new IllegalStateException("draft is closed");
        if (mutationId.equals(mutation)) {
            if (!content.equals(snapshot)) throw new IllegalStateException("mutation content changed");
            return this;
        }
        requireRevision(expectedRevision);
        return new EditorDraft(draftId, ownerId, projectId, trackId, targetClipId, snapshot, baseClip,
                revision + 1, mutation, now, status, null);
    }

    public EditorDraft commit(long expectedRevision, long clipId, Instant now) {
        requireRevision(expectedRevision);
        return new EditorDraft(draftId, ownerId, projectId, trackId, targetClipId, content, baseClip,
                revision + 1, mutationId, now, Status.COMMITTED, clipId);
    }

    public EditorDraft discard(long expectedRevision, Instant now) {
        requireRevision(expectedRevision);
        return new EditorDraft(draftId, ownerId, projectId, trackId, targetClipId, content, baseClip,
                revision + 1, mutationId, now, Status.DISCARDED, null);
    }

    public void requireRevision(long expectedRevision) {
        if (status != Status.ACTIVE || revision != expectedRevision) {
            throw new IllegalStateException("draft revision conflict");
        }
    }
}
