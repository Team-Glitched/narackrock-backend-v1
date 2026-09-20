package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostCommentPort;
import glitched.adlips.application.community.port.out.UserBanQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.community.PostComment;
import java.time.Clock;
import java.time.LocalDateTime;

public class DeletePostCommentUseCase {

    private final PostCommentPort port;
    private final UserBanQueryPort userBanQueryPort;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public DeletePostCommentUseCase(
            PostCommentPort port,
            UserBanQueryPort userBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this.port = port;
        this.userBanQueryPort = userBanQueryPort;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public void execute(Long commentId, Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (userBanQueryPort.isBanned(userId, now)) {
            throw new PostCommentApplicationException(
                    PostCommentErrorCode.BANNED_USER_ACCESS,
                    "현재 서비스 이용 정지 상태이므로 댓글을 삭제할 수 없습니다.");
        }

        transactionRunner.<Void>required(() -> {
            PostComment comment = port.findActiveComment(commentId)
                    .orElseThrow(() -> new PostCommentApplicationException(
                            PostCommentErrorCode.COMMENT_NOT_FOUND,
                            "존재하지 않거나 이미 삭제된 댓글입니다."));
            if (!comment.getUserId().equals(userId)) {
                throw new PostCommentApplicationException(
                        PostCommentErrorCode.NOT_COMMENT_OWNER,
                        "본인이 작성한 댓글만 삭제할 수 있습니다.");
            }
            Long postId = comment.getPostId();
            Long parentCommentId = comment.getParentCommentId();
            comment.delete(now);
            port.update(comment);
            int deletedCount = 1;
            if (parentCommentId == null) {
                deletedCount += port.softDeleteActiveReplies(commentId, now);
            }
            port.adjustPostCommentCount(postId, -deletedCount);
            if (parentCommentId != null) {
                port.adjustParentReplyCount(parentCommentId, -1);
            }
            return null;
        });
    }
}
