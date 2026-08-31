package glitched.adlips.domain.community;

import glitched.adlips.domain.user.User;
import glitched.adlips.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "posts")
public class Post extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gallery_id", nullable = false)
    private Gallery gallery;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "dislike_count", nullable = false)
    private int dislikeCount;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected Post() {
    }

    public Post(Gallery gallery, User user, String title, String content) {
        this.gallery = Objects.requireNonNull(gallery, "gallery must not be null");
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.title = requireTitle(title);
        this.content = requireContent(content);
    }

    private static String requireTitle(String title) {
        Objects.requireNonNull(title, "title must not be null");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        return title;
    }

    private static String requireContent(String content) {
        Objects.requireNonNull(content, "content must not be null");
        if (content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        return content;
    }

    public Long getId() {
        return id;
    }

    public Long getGalleryId() {
        return gallery.getId();
    }

    public Long getUserId() {
        return user.getId();
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public int getViewCount() {
        return viewCount;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public int getDislikeCount() {
        return dislikeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void updateTitleAndContent(String title, String content) {
        this.title = requireTitle(title);
        this.content = requireContent(content);
    }

    public void delete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
