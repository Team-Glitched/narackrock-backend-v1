package glitched.adlips.application.community;

import glitched.adlips.application.notification.NotificationEvents;
import static glitched.adlips.domain.notification.NotificationType.*;

import glitched.adlips.application.community.port.out.PostCommentPort;
import glitched.adlips.application.community.port.out.UserBanQueryPort;
import glitched.adlips.application.community.port.out.GalleryBanQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import java.time.Clock;
import java.time.LocalDateTime;

public class CreatePostCommentUseCase {
    private final NotificationEvents notifications;

    private final PostCommentPort port;
    private final UserBanQueryPort userBanQueryPort;
    private final GalleryBanQueryPort galleryBanQueryPort;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public CreatePostCommentUseCase(
            PostCommentPort port,
            UserBanQueryPort userBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this(port, userBanQueryPort, GalleryBanQueryPort.none(), transactionRunner, clock);
    }

    public CreatePostCommentUseCase(
            PostCommentPort port,
            UserBanQueryPort userBanQueryPort,
            GalleryBanQueryPort galleryBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this(port, userBanQueryPort, galleryBanQueryPort, transactionRunner, clock, NotificationEvents.none());
    }

    public CreatePostCommentUseCase(PostCommentPort port,
            UserBanQueryPort userBanQueryPort,
            GalleryBanQueryPort galleryBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock, NotificationEvents notifications) {
        this.port = port;
        this.userBanQueryPort = userBanQueryPort;
        this.galleryBanQueryPort = galleryBanQueryPort;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    
        this.notifications = notifications;
    }

    public PostCommentResult execute(Long postId, Long userId, String content, Long parentCommentId) {
        if (userBanQueryPort.isBanned(userId, LocalDateTime.now(clock))) {
            throw new PostCommentApplicationException(
                    PostCommentErrorCode.BANNED_USER_ACCESS,
                    "현재 서비스 이용 정지 상태이므로 댓글을 작성할 수 없습니다.");
        }
        boolean isReply = parentCommentId != null;
        if (content == null || content.isBlank()) {
            throw new PostCommentApplicationException(
                    PostCommentErrorCode.INVALID_INPUT_VALUE,
                    isReply ? "대댓글 내용은 필수 입력 사항입니다." : "댓글 내용은 필수 입력 사항입니다.");
        }

        return transactionRunner.required(() -> {
            if (!port.existsActivePost(postId)) {
                throw new PostCommentApplicationException(
                        PostCommentErrorCode.POST_NOT_FOUND,
                        "존재하지 않거나 삭제된 게시글에는 댓글을 작성할 수 없습니다.");
            }
            if (galleryBanQueryPort.isRestrictedForPost(postId, userId, LocalDateTime.now(clock))) {
                throw new PostCommentApplicationException(PostCommentErrorCode.GALLERY_RESTRICTED_USER,
                        "이 갤러리에서 이용이 제한된 상태이므로 댓글을 작성할 수 없습니다.");
            }
            if (isReply && !port.existsActiveParentComment(parentCommentId, postId)) {
                throw new PostCommentApplicationException(
                        PostCommentErrorCode.PARENT_COMMENT_NOT_FOUND,
                        "존재하지 않거나 삭제된 댓글에는 대댓글을 작성할 수 없습니다.");
            }
            Long commentId = port.save(postId, userId, content.trim(), parentCommentId);
            port.adjustPostCommentCount(postId, 1);
            if (isReply) {
                port.adjustParentReplyCount(parentCommentId, 1);
            }
            notifications.record(isReply?POST_REPLY:POST_COMMENT,userId,commentId);
            return new PostCommentResult(postId, commentId, parentCommentId);
        });
    }
}
