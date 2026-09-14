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
        ReactionUpdate update = transactionRunner.required(() -> {
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
            return new ReactionUpdate(
                    isDisliked,
                    new ShortReactionCounts(
                            reactionPort.getLikeCount(shortId),
                            reactionPort.getDislikeCount(shortId)));
        });
        reactionCountCache.put(shortId, update.counts());
        return new ShortDislikeResult(shortId, update.selected(), update.counts().dislikeCount());
    }

    private void validateActiveShort(Long shortId) {
        if (!reactionPort.lockActiveShort(shortId)) {
            throw new ShortReactionApplicationException(
                    ShortReactionErrorCode.SHORT_NOT_FOUND,
                    "숏폼을 찾을 수 없습니다."
            );
        }
    }

    private record ReactionUpdate(boolean selected, ShortReactionCounts counts) {
    }
}
