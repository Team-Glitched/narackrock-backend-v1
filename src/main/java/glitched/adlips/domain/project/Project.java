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
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "projects", indexes = {
        @Index(name = "idx_projects_owner", columnList = "owner_id"),
        @Index(name = "idx_projects_public_published", columnList = "is_public,published_at"),
        @Index(name = "idx_projects_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseTimeEntity {
    public static final String UNTITLED_DRAFT = "제목 없는 곡";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "album_image_file_id", nullable = false)
    private Long albumImageFileId;

    @Column(name = "genre")
    private String genre;

    @Column(name = "bpm", nullable = false)
    private int bpm = 120;

    @Column(name = "song_key")
    private String songKey;

    @Column(name = "time_signature_numerator", nullable = false)
    private int timeSignatureNumerator = 4;

    @Column(name = "time_signature_denominator", nullable = false)
    private int timeSignatureDenominator = 4;

    @Column(name = "ppq", nullable = false)
    private int ppq = 480;

    @Column(name = "max_duration_ms", nullable = false)
    private int maxDurationMs = 60_000;

    @Column(name = "major_version", nullable = false)
    private int majorVersion = 1;

    @Column(name = "minor_version", nullable = false)
    private int minorVersion = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProjectStatus status = ProjectStatus.DRAFT;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public Project(
            User owner,
            String title,
            String description,
            Long albumImageFileId
    ) {
        this.owner = Objects.requireNonNull(
                owner,
                "owner must not be null"
        );
        this.title = Objects.requireNonNull(
                title,
                "title must not be null"
        );
        this.description = description;
        this.albumImageFileId = Objects.requireNonNull(
                albumImageFileId,
                "albumImageFileId must not be null"
        );
    }

    public long getMaxTick() {
        return (long) maxDurationMs * bpm * ppq / 60_000;
    }

    public void updateMetadata(String title, Long albumImageFileId) {
        if (status != ProjectStatus.DRAFT || deletedAt != null) {
            throw new IllegalStateException("only draft metadata can be updated");
        }
        if (title == null || title.isBlank() || albumImageFileId == null || albumImageFileId < 0) {
            throw new IllegalArgumentException("title and a valid album image reference are required");
        }
        this.title = title;
        this.albumImageFileId = albumImageFileId;
    }

    public boolean updateTiming(int bpm, int numerator, int denominator) {
        if (deletedAt != null || (status != ProjectStatus.DRAFT && status != ProjectStatus.IN_PROGRESS)) {
            throw new IllegalStateException("only an editable project can change timing");
        }
        if (bpm < 30 || bpm > 300 || numerator < 1 || numerator > 12
                || (denominator != 2 && denominator != 4 && denominator != 8 && denominator != 16)) {
            throw new IllegalArgumentException("invalid tempo or time signature");
        }
        if (this.bpm == bpm && timeSignatureNumerator == numerator && timeSignatureDenominator == denominator) return false;
        this.bpm = bpm;
        timeSignatureNumerator = numerator;
        timeSignatureDenominator = denominator;
        increaseMinorVersion();
        return true;
    }

    public int tickToMilliseconds(int tick) {
        if (tick < 0) {
            throw new IllegalArgumentException(
                    "tick must not be negative"
            );
        }

        return Math.toIntExact(
                (long) tick * 60_000 / (bpm * ppq)
        );
    }

    public void increaseMajorVersion() {
        majorVersion++;
        minorVersion = 1;
    }

    public void increaseMinorVersion() {
        minorVersion++;
    }

    public String getDisplayVersion() {
        return "v" + majorVersion + "." + minorVersion;
    }

    public void startPublishing() {
        if (deletedAt != null) {
            throw new IllegalStateException("deleted project cannot be published");
        }
        if (status != ProjectStatus.COMPLETED) {
            status = ProjectStatus.IN_PROGRESS;
        }
    }

    public void publish(LocalDateTime publishedAt) {
        if (status != ProjectStatus.IN_PROGRESS && status != ProjectStatus.COMPLETED) {
            throw new IllegalStateException("only exporting project can be published");
        }
        this.publishedAt = Objects.requireNonNull(publishedAt, "publishedAt must not be null");
        this.isPublic = true;
    }

    public void complete() {
        if (status != ProjectStatus.IN_PROGRESS || !isPublic) {
            throw new IllegalStateException("only public in-progress project can be completed");
        }
        status = ProjectStatus.COMPLETED;
    }

    public void delete(LocalDateTime deletedAt) {
        if (this.deletedAt != null) {
            return;
        }
        this.deletedAt = Objects.requireNonNull(deletedAt, "deletedAt must not be null");
        this.status = ProjectStatus.DELETED;
        this.isPublic = false;
    }
}
