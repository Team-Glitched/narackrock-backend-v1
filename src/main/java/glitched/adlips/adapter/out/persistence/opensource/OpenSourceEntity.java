package glitched.adlips.adapter.out.persistence.opensource;

import glitched.adlips.domain.opensource.OpenSourcePublication;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "open_sources", indexes = @Index(name = "idx_open_sources_owner", columnList = "owner_id"))
public class OpenSourceEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "is_public", nullable = false)
    private boolean isPublic;
    @Column(name = "deleted_at")
    private Instant deletedAt;
    // Nullable for existing availability-only rows; application queries exclude incomplete records.
    @Column(name = "owner_id")
    private Long ownerId;
    @Column(length = 100)
    private String title;
    @Column(length = 2000)
    private String description;
    @Column(name = "media_file_id")
    private Long mediaFileId;
    @Column(name = "thumbnail_media_file_id")
    private Long thumbnailMediaFileId;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;

    protected OpenSourceEntity() {}

    void apply(OpenSourcePublication source) {
        ownerId = source.ownerId();
        title = source.title();
        description = source.description();
        mediaFileId = source.mediaFileId();
        thumbnailMediaFileId = source.thumbnailMediaFileId();
        isPublic = source.isPublic();
        createdAt = source.createdAt();
        updatedAt = source.updatedAt();
        deletedAt = source.deletedAt();
    }

    OpenSourcePublication toDomain() {
        return new OpenSourcePublication(id, ownerId, title, description, mediaFileId,
                thumbnailMediaFileId, isPublic, createdAt, updatedAt, deletedAt);
    }
}
