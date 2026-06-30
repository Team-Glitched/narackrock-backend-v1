package glitched.adlips.domain.shorts;

import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.user.User;
import glitched.adlips.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shorts")
public class ShortForm extends BaseCreatedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "project_id")
    private Project project;
    @Column(name = "title", nullable = false)
    private String title;
    @Column(name = "album_image")
    private String albumImage;
    @Column(name = "media_url", nullable = false)
    private String mediaUrl;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false)
    private ShortStatus status = ShortStatus.IN_PROGRESS;
    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;
    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;
    @Column(name = "dislike_count", nullable = false)
    private int dislikeCount = 0;
    @Column(name = "pr_count", nullable = false)
    private int prCount = 0;

    protected ShortForm() {}

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Project getProject() { return project; }
    public String getTitle() { return title; }
    public String getAlbumImage() { return albumImage; }
    public String getMediaUrl() { return mediaUrl; }
    public ShortStatus getStatus() { return status; }
    public int getViewCount() { return viewCount; }
    public int getLikeCount() { return likeCount; }
    public int getDislikeCount() { return dislikeCount; }
    public int getPrCount() { return prCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
