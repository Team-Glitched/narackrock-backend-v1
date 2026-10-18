package glitched.adlips.application.shorts;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.shorts.port.out.ShortCommentReactionPort;
import glitched.adlips.domain.reaction.ReactionType;
import java.util.Optional;

public class ToggleShortCommentDislikeUseCase {
    private final ShortCommentReactionPort reactionPort;
    private final TransactionRunner transactionRunner;

    public ToggleShortCommentDislikeUseCase(ShortCommentReactionPort reactionPort, TransactionRunner transactionRunner) {
        this.reactionPort = reactionPort;
        this.transactionRunner = transactionRunner;
    }

    public ShortCommentDislikeResult toggle(Long userId, Long shortId, Long commentId) {
        return transactionRunner.required(() -> {
            if (!reactionPort.lockActiveComment(commentId, shortId)) {
                throw new ShortCommentApplicationException(ShortCommentErrorCode.COMMENT_NOT_FOUND,
                        "존재하지 않거나 삭제된 댓글에는 싫어요를 누를 수 없습니다.");
            }
            Optional<ReactionType> existing = reactionPort.findReaction(userId, commentId);
            boolean isDisliked;
            if (existing.isEmpty()) {
                reactionPort.saveReaction(userId, commentId, ReactionType.DISLIKE);
                reactionPort.adjustDislikeCount(commentId, 1);
                isDisliked = true;
            } else if (existing.get() == ReactionType.DISLIKE) {
                reactionPort.deleteReaction(userId, commentId);
                reactionPort.adjustDislikeCount(commentId, -1);
                isDisliked = false;
            } else {
                reactionPort.saveReaction(userId, commentId, ReactionType.DISLIKE);
                reactionPort.adjustLikeCount(commentId, -1);
                reactionPort.adjustDislikeCount(commentId, 1);
                isDisliked = true;
            }
            return new ShortCommentDislikeResult(commentId, isDisliked, reactionPort.getDislikeCount(commentId));
        });
    }
}
