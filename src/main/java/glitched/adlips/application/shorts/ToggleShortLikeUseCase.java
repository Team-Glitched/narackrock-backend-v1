package glitched.adlips.application.shorts;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.shorts.port.out.ShortReactionPort;
import glitched.adlips.domain.reaction.ReactionType;
import java.util.Optional;

public class ToggleShortLikeUseCase {

    private final ShortReactionPort reactionPort;
    private final TransactionRunner transactionRunner;

    public ToggleShortLikeUseCase(ShortReactionPort reactionPort, TransactionRunner transactionRunner) {
        this.reactionPort = reactionPort;
        this.transactionRunner = transactionRunner;
    }

    public ShortLikeResult toggle(Long userId, Long shortId) {
        return transactionRunner.required(() -> {
            validateActiveShort(shortId);
            Optional<ReactionType> existing = reactionPort.findReaction(userId, shortId);
            boolean isLiked;
            if (existing.isEmpty()) {
                reactionPort.saveReaction(userId, shortId, ReactionType.LIKE);
                reactionPort.adjustLikeCount(shortId, +1);
                isLiked = true;
            } else if (existing.get() == ReactionType.LIKE) {
                reactionPort.deleteReaction(userId, shortId);
                reactionPort.adjustLikeCount(shortId, -1);
                isLiked = false;
            } else {
                reactionPort.saveReaction(userId, shortId, ReactionType.LIKE);
                reactionPort.adjustDislikeCount(shortId, -1);
                reactionPort.adjustLikeCount(shortId, +1);
                isLiked = true;
            }
            return new ShortLikeResult(shortId, isLiked, reactionPort.getLikeCount(shortId));
        });
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
