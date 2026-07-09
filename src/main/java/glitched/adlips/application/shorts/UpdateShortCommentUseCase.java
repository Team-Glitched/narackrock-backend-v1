package glitched.adlips.application.shorts;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.shorts.port.out.ShortCommentPort;
import glitched.adlips.domain.shorts.ShortComment;

public class UpdateShortCommentUseCase {

    private final ShortCommentPort port;
    private final TransactionRunner transactionRunner;

    public UpdateShortCommentUseCase(ShortCommentPort port, TransactionRunner transactionRunner) {
        this.port = port;
        this.transactionRunner = transactionRunner;
    }

    public Long execute(Long shortId, Long commentId, Long userId, String content) {
        if (content == null || content.isBlank()) {
            throw new ShortCommentApplicationException(
                    ShortCommentErrorCode.INVALID_INPUT_VALUE, "댓글 내용은 필수 입력 사항입니다.");
        }

        return transactionRunner.required(() -> {
            ShortComment comment = port.findActiveComment(commentId, shortId)
                    .orElseThrow(() -> new ShortCommentApplicationException(
                            ShortCommentErrorCode.COMMENT_NOT_FOUND,
                            "수정하려는 댓글을 찾을 수 없거나 이미 삭제되었습니다."));
            if (!comment.getUserId().equals(userId)) {
                throw new ShortCommentApplicationException(
                        ShortCommentErrorCode.NOT_COMMENT_OWNER,
                        "본인이 작성한 댓글만 수정할 수 있습니다.");
            }
            comment.updateContent(content.trim());
            port.updateContent(comment);
            return commentId;
        });
    }
}
