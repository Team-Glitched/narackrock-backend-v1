package glitched.adlips.application.opensource;

import glitched.adlips.domain.reaction.ReactionType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OpenSourceInteractionPort {
    enum Target { SOURCE, COMMENT }
    record ReactionState(boolean isLiked, boolean isDisliked, long likeCount, long dislikeCount) {}
    record CommentRef(Long id, Long openSourceId, Long parentCommentId) {}
    record Comment(Long commentId, Long parentCommentId, OpenSourceQueryPort.Writer writer, String content,
                   long replyCount, boolean isLiked, boolean isDisliked, long likeCount, long dislikeCount, Instant createdAt) {}
    boolean lockAvailableSource(Long sourceId);
    Optional<ReactionType> reaction(Long userId, Target target, Long targetId);
    void setReaction(Long userId, Target target, Long targetId, ReactionType reaction, Instant now);
    ReactionState reactions(Long userId, Target target, Long targetId);
    Optional<CommentRef> findComment(Long id);
    Long insertComment(Long sourceId, Long userId, Long parentId, String content, Instant now);
    Comment comment(Long id, Long viewerId);
    List<Comment> comments(Long sourceId, Long parentId, Long viewerId, int offset, int limit);
    long commentCount(Long sourceId, Long parentId, boolean all);
}
