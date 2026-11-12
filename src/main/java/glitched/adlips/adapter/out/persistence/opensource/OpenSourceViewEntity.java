package glitched.adlips.adapter.out.persistence.opensource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "open_source_views", uniqueConstraints = @UniqueConstraint(
        name = "uk_os_view_source_viewer", columnNames = {"open_source_id", "viewer_id"}))
class OpenSourceViewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "open_source_id", nullable = false)
    private Long openSourceId;

    @Column(name = "viewer_id", nullable = false)
    private Long viewerId;

    @Column(name = "last_viewed_at", nullable = false)
    Instant lastViewedAt;

    @Column(name = "view_count", nullable = false)
    long viewCount;

    protected OpenSourceViewEntity() {}

    OpenSourceViewEntity(Long openSourceId, Long viewerId, Instant viewedAt) {
        this.openSourceId = openSourceId;
        this.viewerId = viewerId;
        this.lastViewedAt = viewedAt;
        this.viewCount = 1;
    }
}
