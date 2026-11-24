package glitched.adlips.application.shorts;

import glitched.adlips.application.notification.NotificationEvents;
import static glitched.adlips.domain.notification.NotificationType.*;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.shorts.port.out.ShortCommentReactionPort;
import glitched.adlips.application.shorts.port.out.UserBanQueryPort;
import glitched.adlips.domain.reaction.ReactionType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

public class ToggleShortCommentLikeUseCase {
    private final NotificationEvents notifications;

    private final ShortCommentReactionPort reactionPort;
    private final TransactionRunner transactionRunner;
    private final UserBanQueryPort userBanQueryPort;
    private final Clock clock;

    public ToggleShortCommentLikeUseCase(ShortCommentReactionPort reactionPort, TransactionRunner transactionRunner) {
        this(reactionPort, transactionRunner, NotificationEvents.none());
    }

    public ToggleShortCommentLikeUseCase(ShortCommentReactionPort reactionPort, TransactionRunner transactionRunner, NotificationEvents notifications) {
        this(reactionPort, transactionRunner, notifications, null, Clock.systemDefaultZone());
    }

    public ToggleShortCommentLikeUseCase(ShortCommentReactionPort reactionPort, TransactionRunner transactionRunner,
                                         NotificationEvents notifications, UserBanQueryPort userBanQueryPort, Clock clock) {
        this.reactionPort = reactionPort;
        this.transactionRunner = transactionRunner;
        this.notifications = notifications;
        this.userBanQueryPort = userBanQueryPort;
        this.clock = clock;
    }

    public ShortCommentLikeResult toggle(Long userId, Long shortId, Long commentId) {
        if (userBanQueryPort != null && userBanQueryPort.isBanned(userId, LocalDateTime.now(clock))) {
            throw new ShortCommentApplicationException(
                    ShortCommentErrorCode.BANNED_USER_ACCESS,
                    "현재 서비스 이용 정지 상태이므로 댓글 반응을 남길 수 없습니다.");
        }
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
            if (isLiked) notifications.record(SHORT_COMMENT_LIKED, userId, commentId);
            return new ShortCommentLikeResult(commentId, isLiked, false,
                    reactionPort.getLikeCount(commentId), reactionPort.getDislikeCount(commentId));
        });
    }
}
