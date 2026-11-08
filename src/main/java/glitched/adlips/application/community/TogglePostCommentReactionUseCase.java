package glitched.adlips.application.community;

import glitched.adlips.application.notification.NotificationEvents;
import static glitched.adlips.domain.notification.NotificationType.*;

import glitched.adlips.application.community.port.out.PostCommentReactionPort;
import glitched.adlips.application.community.port.out.ReactionCounts;
import glitched.adlips.application.community.port.out.UserBanQueryPort;
import glitched.adlips.application.community.port.out.GalleryBanQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.reaction.ReactionToggle;
import glitched.adlips.domain.reaction.ReactionType;
import java.time.Clock;
import java.time.LocalDateTime;

public class TogglePostCommentReactionUseCase {
    private final NotificationEvents notifications;

    private final PostCommentReactionPort reactionPort;
    private final UserBanQueryPort userBanQueryPort;
    private final GalleryBanQueryPort galleryBanQueryPort;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public TogglePostCommentReactionUseCase(
            PostCommentReactionPort reactionPort,
            UserBanQueryPort userBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this(reactionPort, userBanQueryPort, GalleryBanQueryPort.none(), transactionRunner, clock);
    }

    public TogglePostCommentReactionUseCase(
            PostCommentReactionPort reactionPort,
            UserBanQueryPort userBanQueryPort,
            GalleryBanQueryPort galleryBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this(reactionPort, userBanQueryPort, galleryBanQueryPort, transactionRunner, clock, NotificationEvents.none());
    }

    public TogglePostCommentReactionUseCase(PostCommentReactionPort reactionPort,
            UserBanQueryPort userBanQueryPort,
            GalleryBanQueryPort galleryBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock, NotificationEvents notifications) {
        this.reactionPort = reactionPort;
        this.userBanQueryPort = userBanQueryPort;
        this.galleryBanQueryPort = galleryBanQueryPort;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    
        this.notifications = notifications;
    }

    public PostCommentReactionResult toggle(Long userId, Long commentId, ReactionType requested) {
        if (userBanQueryPort.isBanned(userId, LocalDateTime.now(clock))) {
            throw new PostCommentApplicationException(
                    PostCommentErrorCode.BANNED_USER_ACCESS,
                    "현재 서비스 이용 정지 상태이므로 댓글에 반응할 수 없습니다.");
        }
        return transactionRunner.required(() -> {
            if (!reactionPort.lockActiveComment(commentId)) {
                throw new PostCommentApplicationException(
                        PostCommentErrorCode.COMMENT_NOT_FOUND, "존재하지 않거나 삭제된 댓글입니다.");
            }
            if (galleryBanQueryPort.isRestrictedForComment(commentId, userId, LocalDateTime.now(clock))) {
                throw new PostCommentApplicationException(PostCommentErrorCode.GALLERY_RESTRICTED_USER,
                        "이 갤러리에서 이용이 제한된 상태이므로 댓글에 반응할 수 없습니다.");
            }
            ReactionType current = reactionPort.findReaction(userId, commentId).orElse(null);
            ReactionToggle toggle = ReactionToggle.of(current, requested);
            if (toggle.result() == null) {
                reactionPort.deleteReaction(userId, commentId);
            } else {
                reactionPort.saveReaction(userId, commentId, toggle.result());
            }
            reactionPort.adjustReactionCounts(commentId, toggle.likeDelta(), toggle.dislikeDelta());
            ReactionCounts counts = reactionPort.findReactionCounts(commentId);
            if(toggle.result()==ReactionType.LIKE)notifications.record(POST_COMMENT_LIKED,userId,commentId);
            return new PostCommentReactionResult(
                    commentId,
                    toggle.result() == ReactionType.LIKE,
                    toggle.result() == ReactionType.DISLIKE,
                    counts.likeCount(),
                    counts.dislikeCount());
        });
    }
}
