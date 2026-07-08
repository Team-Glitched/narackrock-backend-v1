package glitched.adlips.domain.shorts;

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
@Table(name = "shorts_comments")
public class ShortComment extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shorts_id", nullable = false)
    private ShortForm shorts;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private ShortComment parentComment;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "dislike_count", nullable = false)
    private int dislikeCount;

    @Column(name = "reply_count", nullable = false)
    private int replyCount;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected ShortComment() {
    }

    public ShortComment(ShortForm shorts, User user, String content, ShortComment parentComment) {
        this.shorts = Objects.requireNonNull(shorts, "shorts must not be null");
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.content = requireContent(content);
        this.parentComment = parentComment;
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

    public String getContent() {
        return content;
    }

    public Long getParentCommentId() {
        return parentComment == null ? null : parentComment.getId();
    }
}
