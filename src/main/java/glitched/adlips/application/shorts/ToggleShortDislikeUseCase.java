package glitched.adlips.application.shorts;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.shorts.port.out.ShortReactionPort;
import glitched.adlips.domain.reaction.ReactionType;
import java.util.Optional;

public class ToggleShortDislikeUseCase {

    private final ShortReactionPort reactionPort;
    private final TransactionRunner transactionRunner;

    public ToggleShortDislikeUseCase(ShortReactionPort reactionPort, TransactionRunner transactionRunner) {
        this.reactionPort = reactionPort;
        this.transactionRunner = transactionRunner;
    }

    public ShortDislikeResult toggle(Long userId, Long shortId) {
        return transactionRunner.required(() -> {
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
    }
}
