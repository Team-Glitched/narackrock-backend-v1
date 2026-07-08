package glitched.adlips.application.shorts;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.shorts.port.out.ShortCommentPort;

public class SubmitShortCommentUseCase {

    private final ShortCommentPort port;
    private final TransactionRunner transactionRunner;

    public SubmitShortCommentUseCase(ShortCommentPort port, TransactionRunner transactionRunner) {
        this.port = port;
        this.transactionRunner = transactionRunner;
    }

    public ShortCommentResult execute(Long shortId, Long userId, String content, Long parentCommentId) {
        boolean isReply = parentCommentId != null;
        if (content == null || content.isBlank()) {
            throw new ShortCommentApplicationException(
                    ShortCommentErrorCode.INVALID_INPUT_VALUE,
                    isReply ? "대댓글 내용은 필수 입력 사항입니다." : "댓글 내용은 필수 입력 사항입니다.");
        }

        return transactionRunner.required(() -> {
            if (!port.existsActiveShort(shortId)) {
                throw new ShortCommentApplicationException(
                        ShortCommentErrorCode.SHORT_NOT_FOUND,
                        "존재하지 않거나 삭제된 숏폼에는 댓글을 작성할 수 없습니다.");
            }
            if (isReply && !port.existsActiveParentComment(parentCommentId, shortId)) {
                throw new ShortCommentApplicationException(
                        ShortCommentErrorCode.PARENT_COMMENT_NOT_FOUND,
                        "존재하지 않거나 삭제된 댓글에는 대댓글을 작성할 수 없습니다.");
            }
            Long commentId = port.save(shortId, userId, content.trim(), parentCommentId);
            port.adjustShortCommentCount(shortId, 1);
            if (isReply) {
                port.adjustParentReplyCount(parentCommentId, 1);
            }
            return new ShortCommentResult(shortId, commentId, parentCommentId);
        });
    }
}
