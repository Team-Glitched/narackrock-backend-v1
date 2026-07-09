package glitched.adlips.application.shorts.port.out;

import glitched.adlips.domain.reaction.ReactionType;
import java.util.Optional;

public interface ShortCommentReactionPort {
    boolean lockActiveComment(Long commentId, Long shortId);

    Optional<ReactionType> findReaction(Long userId, Long commentId);

    void saveReaction(Long userId, Long commentId, ReactionType reactionType);

    void deleteReaction(Long userId, Long commentId);

    void adjustLikeCount(Long commentId, int delta);

    void adjustDislikeCount(Long commentId, int delta);

    int getLikeCount(Long commentId);

    int getDislikeCount(Long commentId);
}
