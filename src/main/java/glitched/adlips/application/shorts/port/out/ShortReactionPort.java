package glitched.adlips.application.shorts.port.out;

import glitched.adlips.domain.reaction.ReactionType;
import java.util.Optional;

public interface ShortReactionPort {
    boolean lockActiveShort(Long shortId);
    Optional<ReactionType> findReaction(Long userId, Long shortId);
    void saveReaction(Long userId, Long shortId, ReactionType reactionType);
    void deleteReaction(Long userId, Long shortId);
    void adjustLikeCount(Long shortId, int delta);
    void adjustDislikeCount(Long shortId, int delta);
    int getLikeCount(Long shortId);
    int getDislikeCount(Long shortId);
}
