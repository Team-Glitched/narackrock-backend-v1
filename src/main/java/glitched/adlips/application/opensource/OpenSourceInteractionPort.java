package glitched.adlips.application.opensource;

import glitched.adlips.domain.reaction.ReactionType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OpenSourceInteractionPort {
    enum Target { SOURCE, COMMENT }
    record ReactionState(boolean isLiked, boolean isDisliked, long likeCount, long dislikeCount) {}
    record CommentRef(Long id, Long openSourceId, Long parentCommentId) {}
    record OwnedCommentRef(Long id, Long openSourceId, Long parentCommentId, Long userId) {}
    record Comment(Long commentId, Long parentCommentId, OpenSourceQueryPort.Writer writer, String content,
                   long replyCount, boolean isLiked, boolean isDisliked, long likeCount, long dislikeCount, Instant createdAt) {}
    boolean lockAvailableSource(Long sourceId);
    boolean recordView(Long sourceId, Long viewerId, Instant now, Instant cutoff);
    Optional<ReactionType> reaction(Long userId, Target target, Long targetId);
    void setReaction(Long userId, Target target, Long targetId, ReactionType reaction, Instant now);
    ReactionState reactions(Long userId, Target target, Long targetId);
    Optional<CommentRef> findComment(Long id);
    Optional<OwnedCommentRef> findOwnedComment(Long id);
    Long insertComment(Long sourceId, Long userId, Long parentId, String content, Instant now);
    void updateComment(Long id, String content);
    void deleteComment(Long id, Instant now);
    Comment comment(Long id, Long viewerId);
    List<Comment> comments(Long sourceId, Long parentId, Long viewerId, int offset, int limit);
    boolean isCommentHidden(Long userId, Long commentId);
    void hideComment(Long userId, Long commentId);
    long visibleCommentCount(Long sourceId, Long parentId, Long viewerId, boolean all);
}
