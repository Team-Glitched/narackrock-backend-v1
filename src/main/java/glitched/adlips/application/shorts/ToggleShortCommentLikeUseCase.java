package glitched.adlips.application.shorts;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.shorts.port.out.ShortCommentReactionPort;
import glitched.adlips.domain.reaction.ReactionType;
import java.util.Optional;

public class ToggleShortCommentLikeUseCase {

    private final ShortCommentReactionPort reactionPort;
    private final TransactionRunner transactionRunner;

    public ToggleShortCommentLikeUseCase(ShortCommentReactionPort reactionPort, TransactionRunner transactionRunner) {
        this.reactionPort = reactionPort;
        this.transactionRunner = transactionRunner;
    }

    public ShortCommentLikeResult toggle(Long userId, Long shortId, Long commentId) {
        return transactionRunner.required(() -> {
            if (!reactionPort.lockActiveComment(commentId, shortId)) {
                throw new ShortCommentApplicationException(
                        ShortCommentErrorCode.COMMENT_NOT_FOUND,
                        "존재하지 않거나 이미 삭제된 댓글에는 좋아요를 누를 수 없습니다.");
            }
            Optional<ReactionType> existing = reactionPort.findReaction(userId, commentId);
            boolean isLiked;
            if (existing.isEmpty()) {
                reactionPort.saveReaction(userId, commentId, ReactionType.LIKE);
                reactionPort.adjustLikeCount(commentId, 1);
                isLiked = true;
            } else if (existing.get() == ReactionType.LIKE) {
                reactionPort.deleteReaction(userId, commentId);
                reactionPort.adjustLikeCount(commentId, -1);
                isLiked = false;
            } else {
                reactionPort.saveReaction(userId, commentId, ReactionType.LIKE);
                reactionPort.adjustDislikeCount(commentId, -1);
                reactionPort.adjustLikeCount(commentId, 1);
                isLiked = true;
            }
            return new ShortCommentLikeResult(commentId, isLiked, reactionPort.getLikeCount(commentId));
        });
    }
}
