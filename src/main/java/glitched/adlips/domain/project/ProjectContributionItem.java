package glitched.adlips.domain.project;

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
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_contribution_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_contribution_items_target", columnNames = {"contribution_id", "target_type", "target_id"}),
        indexes = @Index(name = "idx_contribution_items_target", columnList = "target_type,target_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectContributionItem extends BaseCreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contribution_id", nullable = false)
    private ProjectContribution contribution;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private ContributionChangeType changeType = ContributionChangeType.ADD;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private ContributionTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", nullable = false)
    private ContributionItemStatus itemStatus = ContributionItemStatus.PENDING;

    public ProjectContributionItem(ProjectContribution contribution,
                                   ContributionChangeType changeType,
                                   ContributionTargetType targetType,
                                   Long targetId) {
        this.contribution = Objects.requireNonNull(contribution, "contribution must not be null");
        this.changeType = Objects.requireNonNull(changeType, "changeType must not be null");
        this.targetType = Objects.requireNonNull(targetType, "targetType must not be null");
        this.targetId = Objects.requireNonNull(targetId, "targetId must not be null");
        contribution.addItem(this);
    }

    void approve() {
        itemStatus = ContributionItemStatus.APPROVED;
    }

    void reject() {
        itemStatus = ContributionItemStatus.REJECTED;
    }
}
