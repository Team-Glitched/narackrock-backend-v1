package glitched.adlips.domain.project;

import glitched.adlips.domain.user.User;
import glitched.adlips.global.entity.BaseTimeEntity;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_contributions", indexes = {
        @Index(name = "idx_contributions_project_user_status", columnList = "project_id,user_id,approval_status"),
        @Index(name = "idx_contributions_project_status_created", columnList = "project_id,approval_status,created_at"),
        @Index(name = "idx_contributions_user", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectContribution extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_major_version", nullable = false)
    private int baseMajorVersion;

    @Column(name = "base_minor_version", nullable = false)
    private int baseMinorVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    private ContributionApprovalStatus approvalStatus = ContributionApprovalStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewer;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @OneToMany(mappedBy = "contribution")
    private final List<ProjectContributionItem> items = new ArrayList<>();

    public ProjectContribution(Project project, User user, String description) {
        this(project, user, description, project.getMajorVersion(), project.getMinorVersion());
    }

    public ProjectContribution(
            Project project, User user, String description,
            int baseMajorVersion, int baseMinorVersion) {
        this.project = Objects.requireNonNull(project, "project must not be null");
        this.user = Objects.requireNonNull(user, "user must not be null");
        if (baseMajorVersion < 1 || baseMinorVersion < 1) {
            throw new IllegalArgumentException("base project version must be positive");
        }
        this.description = description;
        this.baseMajorVersion = baseMajorVersion;
        this.baseMinorVersion = baseMinorVersion;
    }

    public List<ProjectContributionItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    void addItem(ProjectContributionItem item) {
        items.add(Objects.requireNonNull(item, "item must not be null"));
    }

    public void approve(User reviewer, String reviewComment, LocalDateTime reviewedAt) {
        ensurePending();
        if (requiresVersionConflictCheck() && !isBasedOnCurrentProjectVersion()) {
            throw new ContributionVersionConflictException(
                    baseMajorVersion, baseMinorVersion,
                    project.getMajorVersion(), project.getMinorVersion());
        }
        this.reviewer = Objects.requireNonNull(reviewer, "reviewer must not be null");
        this.reviewedAt = Objects.requireNonNull(reviewedAt, "reviewedAt must not be null");
        this.reviewComment = reviewComment;
        items.forEach(ProjectContributionItem::approve);
        approvalStatus = ContributionApprovalStatus.APPROVED;
        project.increaseMajorVersion();
    }

    public void reject(User reviewer, String reviewComment, LocalDateTime reviewedAt) {
        ensurePending();
        this.reviewer = Objects.requireNonNull(reviewer, "reviewer must not be null");
        this.reviewedAt = Objects.requireNonNull(reviewedAt, "reviewedAt must not be null");
        this.reviewComment = reviewComment;
        items.forEach(ProjectContributionItem::reject);
        approvalStatus = ContributionApprovalStatus.REJECTED;
    }

    public boolean isBasedOnCurrentProjectVersion() {
        return baseMajorVersion == project.getMajorVersion()
                && baseMinorVersion == project.getMinorVersion();
    }

    private boolean requiresVersionConflictCheck() {
        return items.stream()
                .anyMatch(item -> item.getChangeType() != ContributionChangeType.ADD);
    }

    private void ensurePending() {
        if (approvalStatus != ContributionApprovalStatus.PENDING) {
            throw new IllegalStateException("contribution has already been reviewed");
        }
    }
}
