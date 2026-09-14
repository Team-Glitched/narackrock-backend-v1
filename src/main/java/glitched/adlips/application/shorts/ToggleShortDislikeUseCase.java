package glitched.adlips.application.shorts;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.shorts.port.out.ShortReactionPort;
import glitched.adlips.application.shorts.port.out.ShortReactionCountCachePort;
import glitched.adlips.domain.reaction.ReactionType;
import java.util.Optional;

public class ToggleShortDislikeUseCase {

    private final ShortReactionPort reactionPort;
    private final ShortReactionCountCachePort reactionCountCache;
    private final TransactionRunner transactionRunner;

    public ToggleShortDislikeUseCase(
            ShortReactionPort reactionPort,
            ShortReactionCountCachePort reactionCountCache,
            TransactionRunner transactionRunner
    ) {
        this.reactionPort = reactionPort;
        this.reactionCountCache = reactionCountCache;
        this.transactionRunner = transactionRunner;
    }

    public ShortDislikeResult toggle(Long userId, Long shortId) {
        ShortDislikeResult result = transactionRunner.required(() -> {
            validateActiveShort(shortId);
            Optional<ReactionType> existing = reactionPort.findReaction(userId, shortId);
            boolean isDisliked;
            if (existing.isEmpty()) {
                reactionPort.saveReaction(userId, shortId, ReactionType.DISLIKE);
                reactionPort.adjustDislikeCount(shortId, +1);
                isDisliked = true;
            } else if (existing.get() == ReactionType.DISLIKE) {
                reactionPort.deleteReaction(userId, shortId);
                reactionPort.adjustDislikeCount(shortId, -1);
                isDisliked = false;
            } else {
                reactionPort.saveReaction(userId, shortId, ReactionType.DISLIKE);
                reactionPort.adjustLikeCount(shortId, -1);
                reactionPort.adjustDislikeCount(shortId, +1);
                isDisliked = true;
            }
            return new ShortDislikeResult(shortId, isDisliked, reactionPort.getDislikeCount(shortId));
        });
        reactionCountCache.evict(shortId);
        return result;
    }

    private void validateActiveShort(Long shortId) {
        if (!reactionPort.lockActiveShort(shortId)) {
            throw new ShortReactionApplicationException(
                    ShortReactionErrorCode.SHORT_NOT_FOUND,
                    "숏폼을 찾을 수 없습니다."
            );
        }
    }

}
