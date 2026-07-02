package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.application.shorts.port.out.ShortReactionPort;
import glitched.adlips.domain.reaction.Reaction;
import glitched.adlips.domain.reaction.ReactionTargetType;
import glitched.adlips.domain.reaction.ReactionType;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ShortReactionPersistenceAdapter implements ShortReactionPort {

    private final ReactionJpaRepository reactionRepository;
    private final ShortFormJpaRepository shortFormRepository;

    public ShortReactionPersistenceAdapter(
            ReactionJpaRepository reactionRepository,
            ShortFormJpaRepository shortFormRepository
    ) {
        this.reactionRepository = reactionRepository;
        this.shortFormRepository = shortFormRepository;
    }

    @Override
    public Optional<ReactionType> findReaction(Long userId, Long shortId) {
        return reactionRepository
                .findByUserIdAndTargetTypeAndTargetId(userId, ReactionTargetType.SHORT, shortId)
                .map(Reaction::getReactionType);
    }

    @Override
    public void saveReaction(Long userId, Long shortId, ReactionType reactionType) {
        reactionRepository
                .findByUserIdAndTargetTypeAndTargetId(userId, ReactionTargetType.SHORT, shortId)
                .ifPresentOrElse(
                        existing -> existing.changeReactionType(reactionType),
                        () -> reactionRepository.save(
                                Reaction.of(userId, ReactionTargetType.SHORT, shortId, reactionType))
                );
    }

    @Override
    public void deleteReaction(Long userId, Long shortId) {
        reactionRepository.deleteByUserIdAndTargetTypeAndTargetId(
                userId, ReactionTargetType.SHORT, shortId);
    }

    @Override
    public void adjustLikeCount(Long shortId, int delta) {
        shortFormRepository.adjustLikeCount(shortId, delta);
    }

    @Override
    public void adjustDislikeCount(Long shortId, int delta) {
        shortFormRepository.adjustDislikeCount(shortId, delta);
    }

    @Override
    public int getLikeCount(Long shortId) {
        Integer count = shortFormRepository.getLikeCount(shortId);
        return count != null ? count : 0;
    }

    @Override
    public int getDislikeCount(Long shortId) {
        Integer count = shortFormRepository.getDislikeCount(shortId);
        return count != null ? count : 0;
    }
}
