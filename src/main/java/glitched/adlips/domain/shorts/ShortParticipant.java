package glitched.adlips.domain.shorts;

import glitched.adlips.domain.user.User;
import glitched.adlips.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "shorts_participants", uniqueConstraints = @UniqueConstraint(columnNames = {"shorts_id", "user_id", "role"}))
public class ShortParticipant extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shorts_id", nullable = false)
    private ShortForm shorts;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "role", nullable = false)
    private String role;
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    protected ShortParticipant() {}

    public ShortParticipant(ShortForm shorts, User user, String role) {
        this.shorts = Objects.requireNonNull(shorts, "shorts must not be null");
        this.user = Objects.requireNonNull(user, "user must not be null");
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("role must not be blank");
        }
        this.role = role;
    }

    public Long getId() { return id; }
    public ShortForm getShorts() { return shorts; }
    public User getUser() { return user; }
    public String getRole() { return role; }
}
