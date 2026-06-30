package glitched.adlips.domain.user;

import glitched.adlips.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected User() {
    }

    private User(Long id, String email, UserRole role, LocalDateTime deletedAt) {
        this.id = id;
        this.email = requireText(email, "이메일은 필수입니다.");
        this.role = Objects.requireNonNull(role, "권한은 필수입니다.");
        this.deletedAt = deletedAt;
    }

    public static User create(String email) {
        return new User(null, email, UserRole.USER, null);
    }

    public static User restore(Long id, String email, UserRole role, LocalDateTime deletedAt) {
        return new User(Objects.requireNonNull(id), email, role, deletedAt);
    }

    public User withId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("이미 식별자가 할당된 사용자입니다.");
        }
        return new User(Objects.requireNonNull(id), email, role, deletedAt);
    }

    public User withdraw(LocalDateTime withdrawnAt) {
        if (deletedAt != null) {
            return this;
        }
        deletedAt = Objects.requireNonNull(withdrawnAt);
        return this;
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
