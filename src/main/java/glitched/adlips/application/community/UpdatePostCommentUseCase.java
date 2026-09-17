package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostCommentPort;
import glitched.adlips.application.community.port.out.UserBanQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.community.PostComment;
import java.time.Clock;
import java.time.LocalDateTime;

public class UpdatePostCommentUseCase {

    private final PostCommentPort port;
    private final UserBanQueryPort userBanQueryPort;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public UpdatePostCommentUseCase(
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

    public Long execute(Long commentId, Long userId, String content) {
        if (userBanQueryPort.isBanned(userId, LocalDateTime.now(clock))) {
            throw new PostCommentApplicationException(
                    PostCommentErrorCode.BANNED_USER_ACCESS,
                    "현재 서비스 이용 정지 상태이므로 댓글을 수정할 수 없습니다.");
        }
        if (content == null || content.isBlank()) {
            throw new PostCommentApplicationException(
                    PostCommentErrorCode.INVALID_INPUT_VALUE, "댓글 내용은 필수 입력 사항입니다.");
        }

        return transactionRunner.required(() -> {
            PostComment comment = port.findActiveComment(commentId)
                    .orElseThrow(() -> new PostCommentApplicationException(
                            PostCommentErrorCode.COMMENT_NOT_FOUND,
                            "수정하려는 댓글을 찾을 수 없거나 이미 삭제되었습니다."));
            if (!comment.getUserId().equals(userId)) {
                throw new PostCommentApplicationException(
                        PostCommentErrorCode.NOT_COMMENT_OWNER,
                        "본인이 작성한 댓글만 수정할 수 있습니다.");
            }
            comment.updateContent(content.trim());
            port.update(comment);
            return commentId;
        });
    }
}
