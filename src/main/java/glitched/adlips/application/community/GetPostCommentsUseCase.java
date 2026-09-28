package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostCommentQueryPort;
import glitched.adlips.application.port.TransactionRunner;

public class GetPostCommentsUseCase {
    private final PostCommentQueryPort port;
    private final TransactionRunner transactionRunner;

    public GetPostCommentsUseCase(PostCommentQueryPort port, TransactionRunner transactionRunner) {
        this.port = port;
        this.transactionRunner = transactionRunner;
    }

    public PostCommentListResult execute(Long postId, Long viewerId, Long parentCommentId, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw new PostCommentApplicationException(PostCommentErrorCode.INVALID_INPUT_VALUE,
                    "페이지 번호와 크기를 확인해 주세요.");
        }
        return transactionRunner.readOnly(() -> {
            if (!port.existsActivePost(postId)) {
                throw new PostCommentApplicationException(PostCommentErrorCode.POST_NOT_FOUND,
                        "존재하지 않거나 삭제된 게시글입니다.");
            }
            if (parentCommentId != null && !port.existsActiveParentComment(parentCommentId, postId)) {
                throw new PostCommentApplicationException(PostCommentErrorCode.PARENT_COMMENT_NOT_FOUND,
                        "존재하지 않거나 삭제된 부모 댓글입니다.");
            }
            return new PostCommentListResult(postId, parentCommentId, page, size,
                    port.countComments(postId, parentCommentId),
                    port.findComments(postId, viewerId, parentCommentId, page, size));
        });
    }
}
