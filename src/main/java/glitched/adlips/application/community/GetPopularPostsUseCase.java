package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostQueryPort;
import glitched.adlips.application.port.TransactionRunner;

/** 갤러리를 가리지 않고 게시글 목록의 POPULAR 정렬 기준으로 인기글을 조회한다. */
public class GetPopularPostsUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final PostQueryPort port;
    private final TransactionRunner transactionRunner;

    public GetPopularPostsUseCase(PostQueryPort port, TransactionRunner transactionRunner) {
        this.port = port;
        this.transactionRunner = transactionRunner;
    }

    public PopularPostListResult execute(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new PostApplicationException(PostErrorCode.VALIDATION_ERROR, "페이지 번호와 크기를 확인해 주세요.");
        }
        return transactionRunner.readOnly(() -> new PopularPostListResult(
                page, size, port.countAllPosts(), port.findPopularPosts(page, size)));
    }
}
