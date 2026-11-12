package glitched.adlips.application.community;

import glitched.adlips.application.notification.NotificationEvents;
import static glitched.adlips.domain.notification.NotificationType.*;

import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.community.port.out.GalleryModerationLogPort;
import glitched.adlips.application.community.port.out.PostCommentPort;
import glitched.adlips.application.community.port.out.PostPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.community.GalleryActor;
import glitched.adlips.domain.community.GalleryModerationActionType;
import glitched.adlips.domain.community.PostComment;
import java.time.Clock;
import java.time.LocalDateTime;

public class ModeratePostCommentDeletionUseCase {
    private final NotificationEvents notifications;

    private final GalleryAuthorityPort authorityPort;
    private final PostPort postPort;
    private final PostCommentPort commentPort;
    private final GalleryModerationLogPort logPort;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public ModeratePostCommentDeletionUseCase(
            GalleryAuthorityPort authorityPort,
            PostPort postPort,
            PostCommentPort commentPort,
            GalleryModerationLogPort logPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this(authorityPort, postPort, commentPort, logPort, transactionRunner, clock, NotificationEvents.none());
    }

    public ModeratePostCommentDeletionUseCase(GalleryAuthorityPort authorityPort,
            PostPort postPort,
            PostCommentPort commentPort,
            GalleryModerationLogPort logPort,
            TransactionRunner transactionRunner,
            Clock clock, NotificationEvents notifications) {
        this.authorityPort = authorityPort;
        this.postPort = postPort;
        this.commentPort = commentPort;
        this.logPort = logPort;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    
        this.notifications = notifications;
    }

    public void execute(Long actorId, Long galleryId, Long commentId, String reason) {
        transactionRunner.<Void>required(() -> {
            LocalDateTime now = LocalDateTime.now(clock);
            GalleryModerationGuard.requireGallery(authorityPort, galleryId);
            GalleryActor actor = GalleryModerationGuard.requireManager(authorityPort, actorId, galleryId, now);
            GalleryModerationGuard.requireReason(reason);
            PostComment comment = commentPort.findActiveComment(commentId)
                    .filter(found -> postPort.findActivePost(found.getPostId(), galleryId).isPresent())
                    .orElseThrow(() -> new GalleryModerationApplicationException(
                            GalleryModerationErrorCode.COMMENT_NOT_FOUND, "이 갤러리에 존재하지 않거나 이미 삭제된 댓글입니다."));
            GalleryModerationGuard.requireCanDeleteContentOf(authorityPort, actor, comment.getUserId(), galleryId, now);

            PostCommentDeletion.delete(commentPort, commentId, comment, now);
            logPort.save(galleryId, actorId, GalleryModerationActionType.DELETE_COMMENT,
                    comment.getUserId(), commentId, reason.trim());
            notifications.record(POST_COMMENT_MODERATED,actorId,commentId,null,reason);
            return null;
        });
    }
}
