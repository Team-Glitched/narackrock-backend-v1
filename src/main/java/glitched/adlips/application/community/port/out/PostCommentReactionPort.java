package glitched.adlips.application.community.port.out;

import glitched.adlips.domain.reaction.ReactionType;
import java.util.Optional;

public interface PostCommentReactionPort {
    boolean lockActiveComment(Long commentId);
    Optional<ReactionType> findReaction(Long userId, Long commentId);
    void saveReaction(Long userId, Long commentId, ReactionType reactionType);
    void deleteReaction(Long userId, Long commentId);
    void adjustReactionCounts(Long commentId, int likeDelta, int dislikeDelta);
    ReactionCounts findReactionCounts(Long commentId);
}
