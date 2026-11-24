package glitched.adlips.domain.community;

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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "gallery_proposals", indexes = {
        @Index(name = "idx_gallery_proposals_status_created", columnList = "status, created_at"),
        @Index(name = "idx_gallery_proposals_user", columnList = "user_id")
})
public class GalleryProposal extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User proposer;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GalleryProposalStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    protected GalleryProposal() {
    }

    private GalleryProposal(User proposer, String name, String description) {
        this.proposer = Objects.requireNonNull(proposer, "proposer must not be null");
        this.name = Gallery.normalizeName(name);
        this.description = Gallery.normalizeDescription(description);
        this.status = GalleryProposalStatus.PENDING;
    }

    public static GalleryProposal submit(User proposer, String name, String description) {
        return new GalleryProposal(proposer, name, description);
    }

    public void approve(User reviewer, LocalDateTime reviewedAt) {
        requirePending();
        markReviewed(reviewer, reviewedAt);
        this.status = GalleryProposalStatus.APPROVED;
    }

    public void reject(User reviewer, String reason, LocalDateTime reviewedAt) {
        requirePending();
        String sanitized = glitched.adlips.domain.common.TextSanitizer.sanitizeMultiline(reason);
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("반려 사유는 필수입니다.");
        }
        markReviewed(reviewer, reviewedAt);
        this.rejectionReason = sanitized;
        this.status = GalleryProposalStatus.REJECTED;
    }

    public boolean isPending() {
        return status == GalleryProposalStatus.PENDING;
    }

    private void requirePending() {
        if (!isPending()) {
            throw new IllegalStateException("이미 검토된 갤러리 개설 요청입니다.");
        }
    }

    private void markReviewed(User reviewer, LocalDateTime reviewedAt) {
        this.reviewedBy = Objects.requireNonNull(reviewer, "reviewer must not be null");
        this.reviewedAt = Objects.requireNonNull(reviewedAt, "reviewedAt must not be null");
    }

    public Long getId() {
        return id;
    }

    public Long getProposerId() {
        return proposer.getId();
    }

    public User getProposer() {
        return proposer;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public GalleryProposalStatus getStatus() {
        return status;
    }

    public Long getReviewedById() {
        return reviewedBy == null ? null : reviewedBy.getId();
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}
