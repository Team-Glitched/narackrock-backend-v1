package glitched.adlips.domain.admin;

import glitched.adlips.domain.user.User;
import glitched.adlips.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_bans", indexes = {
        @Index(name = "idx_user_bans_user_lifted", columnList = "user_id, lifted_at"),
        @Index(name = "idx_user_bans_admin", columnList = "admin_id")
})
public class UserBan extends BaseCreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "banned_until")
    private LocalDateTime bannedUntil;

    @Column(name = "lifted_at")
    private LocalDateTime liftedAt;

    protected UserBan() {
    }

    private UserBan(Long id, User user, User admin, String reason, LocalDateTime bannedUntil, LocalDateTime liftedAt) {
        this.id = id;
        this.user = Objects.requireNonNull(user, "차단 대상 유저는 필수입니다.");
        this.admin = Objects.requireNonNull(admin, "관리자는 필수입니다.");
        this.reason = requireReason(reason);
        this.bannedUntil = bannedUntil;
        this.liftedAt = liftedAt;
    }

    public static UserBan create(User user, User admin, String reason, LocalDateTime bannedUntil) {
        return new UserBan(null, user, admin, reason, bannedUntil, null);
    }

    public static UserBan restore(
            Long id,
            User user,
            User admin,
            String reason,
            LocalDateTime bannedUntil,
            LocalDateTime liftedAt
    ) {
        return new UserBan(id, user, admin, reason, bannedUntil, liftedAt);
    }

    public UserBan lift(LocalDateTime liftedAt) {
        if (this.liftedAt == null) {
            this.liftedAt = Objects.requireNonNull(liftedAt);
        }
        return this;
    }

    public boolean isActiveAt(LocalDateTime now) {
        return liftedAt == null && (bannedUntil == null || bannedUntil.isAfter(now));
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public User getAdmin() {
        return admin;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getBannedUntil() {
        return bannedUntil;
    }

    public LocalDateTime getLiftedAt() {
        return liftedAt;
    }

    private static String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("차단 사유는 필수입니다.");
        }
        return reason.trim();
    }
}
