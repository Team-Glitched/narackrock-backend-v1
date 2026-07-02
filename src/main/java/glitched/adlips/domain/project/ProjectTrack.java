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
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_tracks", indexes = {
        @Index(name = "idx_tracks_project_sort", columnList = "project_id,sort_order"),
        @Index(name = "idx_tracks_project_approval", columnList = "project_id,approval_status"),
        @Index(name = "idx_tracks_owner", columnList = "owner_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectTrack extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "name")
    private String name;

    @Column(name = "instrument")
    private String instrument;

    @Column(name = "media_file_id")
    private Long mediaFileId;

    @Column(name = "volume", nullable = false)
    private int volume = 100;

    @Column(name = "pan", nullable = false)
    private int pan;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_muted", nullable = false)
    private boolean isMuted;

    @Column(name = "is_solo", nullable = false)
    private boolean isSolo;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.DRAFT;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    public ProjectTrack(Project project, User owner, String name, String instrument, int sortOrder) {
        this.project = Objects.requireNonNull(project, "project must not be null");
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
        this.name = name;
        this.instrument = instrument;
        this.sortOrder = sortOrder;
    }

    public boolean belongsTo(Project project) {
        return this.project == project
                || (this.project.getId() != null && this.project.getId().equals(project.getId()));
    }

    public void replaceRenderedMedia(Long mediaFileId) {
        this.mediaFileId = Objects.requireNonNull(mediaFileId, "mediaFileId must not be null");
    }

    public void invalidateRenderedMedia() {
        this.mediaFileId = null;
    }

    public void changeVolume(int volume) {
        if (volume < 0 || volume > 100) {
            throw new IllegalArgumentException("volume must be between 0 and 100");
        }
        if (this.volume == volume) {
            return;
        }
        if (approvalStatus == ApprovalStatus.APPROVED) {
            project.increaseMinorVersion();
        }
        this.volume = volume;
        invalidateRenderedMedia();
    }

    public void delete() {
        if (isDeleted) {
            return;
        }
        if (approvalStatus == ApprovalStatus.APPROVED) {
            project.increaseMinorVersion();
        }
        isDeleted = true;
        mediaFileId = null;
    }

    public void approve() {
        approvalStatus = ApprovalStatus.APPROVED;
    }

    public void submitForReview() {
        if (approvalStatus != ApprovalStatus.DRAFT) {
            throw new IllegalStateException("only draft track can be submitted");
        }
        approvalStatus = ApprovalStatus.PENDING;
    }

    public void reject() {
        approvalStatus = ApprovalStatus.REJECTED;
    }
}
