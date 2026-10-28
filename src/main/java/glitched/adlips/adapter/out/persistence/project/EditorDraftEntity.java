package glitched.adlips.adapter.out.persistence.project;

import glitched.adlips.domain.project.EditorDraft;
import glitched.adlips.domain.project.EditorDraftSnapshot;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "editor_drafts", indexes = {
        @Index(name = "idx_editor_drafts_owner_project_status", columnList = "owner_id,project_id,status")
})
public class EditorDraftEntity {
    @Id private UUID id;
    @Column(name = "owner_id", nullable = false) private long ownerId;
    @Column(name = "project_id", nullable = false) private long projectId;
    @Column(name = "track_id", nullable = false) private long trackId;
    @Column(name = "target_clip_id") private Long targetClipId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private EditorDraftSnapshot content;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "base_clip") private EditorDraftSnapshot baseClip;
    @Column(nullable = false) private long revision;
    @Column(name = "mutation_id", nullable = false) private UUID mutationId;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private EditorDraft.Status status;
    @Column(name = "committed_clip_id") private Long committedClipId;

    protected EditorDraftEntity() {}

    EditorDraftEntity(EditorDraft draft) {
        id = draft.draftId(); ownerId = draft.ownerId(); projectId = draft.projectId(); trackId = draft.trackId();
        targetClipId = draft.targetClipId(); content = draft.content(); baseClip = draft.baseClip();
        revision = draft.revision(); mutationId = draft.mutationId(); updatedAt = draft.updatedAt();
        status = draft.status(); committedClipId = draft.committedClipId();
    }

    EditorDraft toDomain() {
        return new EditorDraft(id, ownerId, projectId, trackId, targetClipId, content, baseClip,
                revision, mutationId, updatedAt, status, committedClipId);
    }
}
