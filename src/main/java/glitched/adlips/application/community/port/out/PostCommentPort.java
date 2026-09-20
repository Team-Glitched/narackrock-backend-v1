package glitched.adlips.application.community.port.out;

import glitched.adlips.domain.community.PostComment;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PostCommentPort {
    boolean existsActivePost(Long postId);

    boolean existsActiveParentComment(Long parentCommentId, Long postId);

    Long save(Long postId, Long userId, String content, Long parentCommentId);

    void adjustPostCommentCount(Long postId, int delta);

    void adjustParentReplyCount(Long parentCommentId, int delta);

    int softDeleteActiveReplies(Long parentCommentId, LocalDateTime deletedAt);

    Optional<PostComment> findActiveComment(Long commentId);

    void update(PostComment comment);
}
