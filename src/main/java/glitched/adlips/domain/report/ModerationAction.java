package glitched.adlips.domain.report;

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
@Table(name = "moderation_actions",
        indexes = {
                @Index(name = "idx_moderation_actions_report_id", columnList = "report_id"),
                @Index(name = "idx_moderation_actions_admin_id", columnList = "admin_id"),
                @Index(name = "idx_moderation_actions_target", columnList = "target_type, target_id")
        })
public class ModerationAction extends BaseCreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ModerationActionType actionType;

    @Column(name = "reason")
    private String reason;

    protected ModerationAction() {
    }

    public ModerationAction(
            Report report, User admin, ReportTargetType targetType, Long targetId,
            ModerationActionType actionType, String reason
    ) {
        this.report = report;
        this.admin = Objects.requireNonNull(admin, "admin must not be null");
        this.targetType = Objects.requireNonNull(targetType, "targetType must not be null");
        this.targetId = Objects.requireNonNull(targetId, "targetId must not be null");
        this.actionType = Objects.requireNonNull(actionType, "actionType must not be null");
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public Report getReport() {
        return report;
    }

    public User getAdmin() {
        return admin;
    }

    public ReportTargetType getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public ModerationActionType getActionType() {
        return actionType;
    }

    public String getReason() {
        return reason;
    }
}
