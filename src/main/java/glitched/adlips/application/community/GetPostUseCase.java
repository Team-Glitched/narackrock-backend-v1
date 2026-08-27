package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostQueryItem;
import glitched.adlips.application.community.port.out.PostQueryPort;
import glitched.adlips.application.port.TransactionRunner;

public class GetPostUseCase {

    private final PostQueryPort port;
    private final TransactionRunner transactionRunner;

    public GetPostUseCase(PostQueryPort port) {
        this(port, TransactionRunner.direct());
    }

    public GetPostUseCase(PostQueryPort port, TransactionRunner transactionRunner) {
        this.port = port;
        this.transactionRunner = transactionRunner;
    }

    public PostQueryItem execute(Long postId, Long viewerId) {
        return transactionRunner.readOnly(() -> port.findDetail(postId, viewerId))
                .orElseThrow(() -> new PostApplicationException(
                        PostErrorCode.POST_NOT_FOUND, "존재하지 않거나 이미 삭제된 게시글입니다."));
    }
}
