package glitched.adlips.domain.shorts;

import glitched.adlips.domain.project.Project;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;

@Entity
@Table(name = "shorts")
@Getter
public class ShortForm extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "export_id", unique = true)
    private Long exportId;

    @Column(nullable = false)
    private String title;

    @Column(name = "album_image_file_id")
    private Long albumImageFileId;

    @Column(name = "media_file_id", nullable = false)
    private Long mediaFileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShortStatus status = ShortStatus.IN_PROGRESS;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "dislike_count", nullable = false)
    private int dislikeCount;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    @Column(name = "contribution_count", nullable = false)
    private int contributionCount;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected ShortForm() {
    }

    public static ShortForm completedFromExport(
            Project project,
            Long exportId,
            Long mixedAudioFileId
    ) {
        Objects.requireNonNull(project, "project must not be null");
        ShortForm shortForm = new ShortForm();
        shortForm.user = project.getOwner();
        shortForm.project = project;
        shortForm.exportId = Objects.requireNonNull(exportId, "exportId must not be null");
        shortForm.title = project.getTitle();
        shortForm.albumImageFileId = project.getAlbumImageFileId();
        shortForm.mediaFileId = Objects.requireNonNull(
                mixedAudioFileId, "mixedAudioFileId must not be null");
        shortForm.status = ShortStatus.COMPLETED;
        return shortForm;
    }
}
