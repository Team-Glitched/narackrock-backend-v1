package glitched.adlips.domain.shorts;

import glitched.adlips.domain.user.User;
import glitched.adlips.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "shorts_reports",
        uniqueConstraints = @UniqueConstraint(columnNames = {"reporter_id", "shorts_id"}))
public class ShortReport extends BaseCreatedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "reporter_id", nullable = false) private User reporter;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shorts_id", nullable = false) private ShortForm shorts;
    @Column(name = "reason", nullable = false) private String reason;
    @Column(name = "description", columnDefinition = "TEXT") private String description;

    protected ShortReport() {
    }

    public ShortReport(User reporter, ShortForm shorts, String reason, String description) {
        this.reporter = Objects.requireNonNull(reporter, "reporter must not be null");
        this.shorts = Objects.requireNonNull(shorts, "shorts must not be null");
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
        this.description = description;
    }

    public Long getId() {
        return id;
    }
}
