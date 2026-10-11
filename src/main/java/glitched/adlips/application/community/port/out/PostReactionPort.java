package glitched.adlips.application.community.port.out;

import glitched.adlips.domain.reaction.ReactionType;
import java.util.Optional;

public interface PostReactionPort {
    boolean lockActivePost(Long postId);
    Optional<ReactionType> findReaction(Long userId, Long postId);
    void saveReaction(Long userId, Long postId, ReactionType reactionType);
    void deleteReaction(Long userId, Long postId);
    void adjustReactionCounts(Long postId, int likeDelta, int dislikeDelta);
    ReactionCounts findReactionCounts(Long postId);
}
