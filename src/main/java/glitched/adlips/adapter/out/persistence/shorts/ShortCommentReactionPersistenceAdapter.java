package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.application.shorts.port.out.ShortCommentReactionPort;
import glitched.adlips.domain.reaction.Reaction;
import glitched.adlips.domain.reaction.ReactionTargetType;
import glitched.adlips.domain.reaction.ReactionType;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ShortCommentReactionPersistenceAdapter implements ShortCommentReactionPort {

    private final ReactionJpaRepository reactionRepository;
    private final ShortCommentJpaRepository shortCommentRepository;

    public ShortCommentReactionPersistenceAdapter(
            ReactionJpaRepository reactionRepository,
            ShortCommentJpaRepository shortCommentRepository
    ) {
        this.reactionRepository = reactionRepository;
        this.shortCommentRepository = shortCommentRepository;
    }

    @Override
    public boolean lockActiveComment(Long commentId, Long shortId) {
        return shortCommentRepository.findActiveByIdAndShortsIdForUpdate(commentId, shortId).isPresent();
    }

    @Override
    public Optional<ReactionType> findReaction(Long userId, Long commentId) {
        return reactionRepository
                .findByUserIdAndTargetTypeAndTargetId(userId, ReactionTargetType.SHORT_COMMENT, commentId)
                .map(Reaction::getReactionType);
    }

    @Override
    public void saveReaction(Long userId, Long commentId, ReactionType reactionType) {
        reactionRepository
                .findByUserIdAndTargetTypeAndTargetId(userId, ReactionTargetType.SHORT_COMMENT, commentId)
                .ifPresentOrElse(
                        existing -> existing.changeReactionType(reactionType),
                        () -> reactionRepository.save(
                                Reaction.of(userId, ReactionTargetType.SHORT_COMMENT, commentId, reactionType))
                );
    }

    @Override
    public void deleteReaction(Long userId, Long commentId) {
        reactionRepository.deleteByUserIdAndTargetTypeAndTargetId(
                userId, ReactionTargetType.SHORT_COMMENT, commentId);
    }

    @Override
    public void adjustLikeCount(Long commentId, int delta) {
        shortCommentRepository.adjustLikeCount(commentId, delta);
    }

    @Override
    public void adjustDislikeCount(Long commentId, int delta) {
        shortCommentRepository.adjustDislikeCount(commentId, delta);
    }

    @Override
    public int getLikeCount(Long commentId) {
        Integer count = shortCommentRepository.getLikeCount(commentId);
        return count != null ? count : 0;
    }

    @Override
    public int getDislikeCount(Long commentId) {
        Integer count = shortCommentRepository.getDislikeCount(commentId);
        return count != null ? count : 0;
    }
}
