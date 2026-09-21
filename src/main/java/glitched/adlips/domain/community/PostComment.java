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
@Table(name = "post_comments")
public class PostComment extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private PostComment parentComment;

    @Column(name = "reply_count", nullable = false)
    private int replyCount;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected PostComment() {
    }

    public PostComment(Post post, User user, String content, PostComment parentComment) {
        this.post = Objects.requireNonNull(post, "post must not be null");
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

    public void updateContent(String content) {
        this.content = requireContent(content);
    }

    public void delete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getPostId() {
        return post.getId();
    }

    public Long getUserId() {
        return user.getId();
    }

    public String getContent() {
        return content;
    }

    public Long getParentCommentId() {
        return parentComment == null ? null : parentComment.getId();
    }

    public int getReplyCount() {
        return replyCount;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
