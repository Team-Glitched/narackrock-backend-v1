package glitched.adlips.domain.community;

import glitched.adlips.domain.user.User;
import glitched.adlips.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "gallery_bans", indexes = {
        @Index(name = "idx_gallery_bans_gallery_user_lifted", columnList = "gallery_id, user_id, lifted_at"),
        @Index(name = "idx_gallery_bans_gallery_created", columnList = "gallery_id, created_at")
})
public class GalleryBan extends BaseCreatedEntity {
    public static final int MIN_SUSPEND_DAYS = 1;
    public static final int MAX_SUSPEND_DAYS = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gallery_id", nullable = false)
    private Gallery gallery;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "ban_type", nullable = false)
    private GalleryBanType banType;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "banned_until")
    private LocalDateTime bannedUntil;

    @Column(name = "lifted_at")
    private LocalDateTime liftedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lifted_by")
    private User liftedBy;

    protected GalleryBan() {
    }

    private GalleryBan(
            Gallery gallery, User user, User actor, GalleryBanType banType, String reason, LocalDateTime bannedUntil
    ) {
        this.gallery = Objects.requireNonNull(gallery, "gallery must not be null");
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.actor = Objects.requireNonNull(actor, "actor must not be null");
        this.banType = banType;
        this.reason = requireReason(reason);
        this.bannedUntil = bannedUntil;
    }

    public static GalleryBan suspend(
            Gallery gallery, User user, User actor, String reason, int days, LocalDateTime now
    ) {
        if (days < MIN_SUSPEND_DAYS || days > MAX_SUSPEND_DAYS) {
            throw new IllegalArgumentException("이용정지 기간은 1일 이상 30일 이하여야 합니다.");
        }
        Objects.requireNonNull(now, "now must not be null");
        return new GalleryBan(gallery, user, actor, GalleryBanType.SUSPEND, reason, now.plusDays(days));
    }

    public static GalleryBan expel(Gallery gallery, User user, User actor, String reason) {
        return new GalleryBan(gallery, user, actor, GalleryBanType.EXPEL, reason, null);
    }

    public void lift(User liftedBy, LocalDateTime liftedAt) {
        if (this.liftedAt != null) {
            return;
        }
        this.liftedBy = Objects.requireNonNull(liftedBy, "liftedBy must not be null");
        this.liftedAt = Objects.requireNonNull(liftedAt, "liftedAt must not be null");
    }

    public boolean isActiveAt(LocalDateTime now) {
        return liftedAt == null && (bannedUntil == null || bannedUntil.isAfter(now));
    }

    private static String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("제재 사유는 필수입니다.");
        }
        return reason.trim();
    }

    public Long getId() {
        return id;
    }

    public Long getGalleryId() {
        return gallery.getId();
    }

    public Long getUserId() {
        return user.getId();
    }

    public Long getActorId() {
        return actor.getId();
    }

    public GalleryBanType getBanType() {
        return banType;
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

    public Long getLiftedById() {
        return liftedBy == null ? null : liftedBy.getId();
    }
}
