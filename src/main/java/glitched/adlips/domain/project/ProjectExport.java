package glitched.adlips.domain.project;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_exports", indexes = {
        @Index(name = "idx_exports_project", columnList = "project_id"),
        @Index(name = "idx_exports_user", columnList = "user_id"),
        @Index(name = "idx_exports_status", columnList = "status"),
        @Index(name = "idx_exports_project_status_created", columnList = "project_id,status,created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectExport extends BaseCreatedEntity {
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

    @Column(name = "project_major_version", nullable = false)
    private int projectMajorVersion;

    @Column(name = "project_minor_version", nullable = false)
    private int projectMinorVersion;

    @Column(name = "media_file_id")
    private Long mediaFileId;

    @Column(name = "layer_archive_file_id")
    private Long layerArchiveFileId;

    @Column(name = "short_id")
    private Long shortId;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExportStatus status = ExportStatus.QUEUED;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public ProjectExport(Project project, User user) {
        this.project = Objects.requireNonNull(project, "project must not be null");
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.projectMajorVersion = project.getMajorVersion();
        this.projectMinorVersion = project.getMinorVersion();
    }

    public void startProcessing() {
        if (status != ExportStatus.QUEUED) {
            throw new IllegalStateException("only queued exports can start processing");
        }
        status = ExportStatus.PROCESSING;
    }

    public void complete(
            Long mediaFileId,
            Long layerArchiveFileId,
            Long shortId,
            int durationMs,
            LocalDateTime completedAt
    ) {
        if (status != ExportStatus.PROCESSING) {
            throw new IllegalStateException("only processing exports can complete");
        }
        if (durationMs <= 0) {
            throw new IllegalArgumentException("durationMs must be positive");
        }
        this.mediaFileId = Objects.requireNonNull(mediaFileId, "mediaFileId must not be null");
        this.layerArchiveFileId = Objects.requireNonNull(
                layerArchiveFileId, "layerArchiveFileId must not be null");
        this.shortId = Objects.requireNonNull(shortId, "shortId must not be null");
        this.durationMs = durationMs;
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt must not be null");
        this.status = ExportStatus.COMPLETED;
        this.errorMessage = null;
        project.completePublishing(completedAt);
    }

    public void fail(String errorMessage, LocalDateTime completedAt) {
        if (status != ExportStatus.PROCESSING) {
            throw new IllegalStateException("only processing exports can fail");
        }
        this.errorMessage = Objects.requireNonNull(errorMessage, "errorMessage must not be null");
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt must not be null");
        this.status = ExportStatus.FAILED;
    }
}
