package glitched.adlips.adapter.out.persistence.opensource;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "open_source_saves", indexes = @Index(name = "idx_open_source_saves_user_created", columnList = "user_id,saved_at,open_source_id"), uniqueConstraints = @UniqueConstraint(
        name = "uk_open_source_save_user_source", columnNames = {"user_id", "open_source_id"}))
public class OpenSourceSaveEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    // Keep the reference when a source is removed so its owner can clear the saved entry.
    @Column(name = "open_source_id", nullable = false)
    private Long openSourceId;
    @Column(name = "saved_at", nullable = false, updatable = false)
    private Instant savedAt;

    protected OpenSourceSaveEntity() {}

    OpenSourceSaveEntity(Long userId, Long openSourceId) {
        this.userId = userId;
        this.openSourceId = openSourceId;
        this.savedAt = Instant.now();
    }
}
