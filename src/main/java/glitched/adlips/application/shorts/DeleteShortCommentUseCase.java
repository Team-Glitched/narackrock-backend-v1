package glitched.adlips.application.shorts;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.shorts.port.out.ShortCommentPort;
import glitched.adlips.domain.shorts.ShortComment;
import java.time.Clock;
import java.time.LocalDateTime;

public class DeleteShortCommentUseCase {

    private final ShortCommentPort port;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public DeleteShortCommentUseCase(ShortCommentPort port, TransactionRunner transactionRunner, Clock clock) {
        this.port = port;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public void execute(Long shortId, Long commentId, Long userId) {
        transactionRunner.<Void>required(() -> {
            ShortComment comment = port.findActiveComment(commentId, shortId)
                    .orElseThrow(() -> new ShortCommentApplicationException(
                            ShortCommentErrorCode.COMMENT_NOT_FOUND,
                            "존재하지 않거나 이미 삭제된 댓글입니다."));
            if (!comment.getUserId().equals(userId)) {
                throw new ShortCommentApplicationException(
                        ShortCommentErrorCode.NOT_COMMENT_OWNER,
                        "본인이 작성한 댓글만 삭제할 수 있습니다.");
            }
            Long parentCommentId = comment.getParentCommentId();
            comment.delete(LocalDateTime.now(clock));
            port.updateContent(comment);
            port.adjustShortCommentCount(shortId, -1);
            if (parentCommentId != null) {
                port.adjustParentReplyCount(parentCommentId, -1);
            }
            return null;
        });
    }
}
