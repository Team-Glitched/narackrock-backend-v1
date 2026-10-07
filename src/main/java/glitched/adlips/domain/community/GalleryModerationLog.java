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
import java.util.Objects;

@Entity
@Table(name = "gallery_moderation_logs",
        indexes = @Index(name = "idx_gallery_moderation_logs_gallery_created", columnList = "gallery_id, created_at"))
public class GalleryModerationLog extends BaseCreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gallery_id", nullable = false)
    private Gallery gallery;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private GalleryModerationActionType actionType;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "reason")
    private String reason;

    protected GalleryModerationLog() {
    }

    private GalleryModerationLog(
            Gallery gallery, User actor, GalleryModerationActionType actionType,
            Long targetUserId, Long targetId, String reason
    ) {
        this.gallery = Objects.requireNonNull(gallery, "gallery must not be null");
        this.actor = Objects.requireNonNull(actor, "actor must not be null");
        this.actionType = Objects.requireNonNull(actionType, "actionType must not be null");
        this.targetUserId = targetUserId;
        this.targetId = targetId;
        this.reason = reason == null ? null : reason.trim();
    }

    public static GalleryModerationLog record(
            Gallery gallery, User actor, GalleryModerationActionType actionType,
            Long targetUserId, Long targetId, String reason
    ) {
        return new GalleryModerationLog(gallery, actor, actionType, targetUserId, targetId, reason);
    }

    public Long getId() {
        return id;
    }

    public Long getGalleryId() {
        return gallery.getId();
    }

    public Long getActorId() {
        return actor.getId();
    }

    public GalleryModerationActionType getActionType() {
        return actionType;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getReason() {
        return reason;
    }
}
