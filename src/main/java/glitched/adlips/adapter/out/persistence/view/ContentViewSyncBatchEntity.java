package glitched.adlips.adapter.out.persistence.view;

import glitched.adlips.application.view.ContentViewKey;
import glitched.adlips.application.view.ContentViewTarget;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "content_view_sync_batches")
class ContentViewSyncBatchEntity {

    @Id
    @Column(name = "batch_id", length = 64)
    private String batchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 16)
    private ContentViewTarget target;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ContentViewSyncBatchEntity() {
    }

    ContentViewSyncBatchEntity(String batchId, ContentViewKey key, long viewCount) {
        this.batchId = batchId;
        this.target = key.target();
        this.contentId = key.contentId();
        this.viewCount = viewCount;
        this.createdAt = LocalDateTime.now();
    }
}
