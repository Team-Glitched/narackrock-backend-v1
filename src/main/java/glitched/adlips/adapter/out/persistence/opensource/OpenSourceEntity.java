package glitched.adlips.adapter.out.persistence.opensource;

import jakarta.persistence.*;
import java.time.Instant;

/** Availability record; content publishing is a separate, not yet specified workflow. */
@Entity
@Table(name = "open_sources")
public class OpenSourceEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected OpenSourceEntity() {}
}
