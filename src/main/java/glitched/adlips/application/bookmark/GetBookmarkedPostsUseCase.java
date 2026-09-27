package glitched.adlips.application.bookmark;

import glitched.adlips.application.bookmark.port.out.BookmarkQueryPort;
import glitched.adlips.application.port.TransactionRunner;

public class GetBookmarkedPostsUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final BookmarkQueryPort port;
    private final TransactionRunner transactionRunner;

    public GetBookmarkedPostsUseCase(BookmarkQueryPort port, TransactionRunner transactionRunner) {
        this.port = port;
        this.transactionRunner = transactionRunner;
    }

    public BookmarkedPostListResult execute(Long userId, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BookmarkApplicationException(
                    BookmarkErrorCode.INVALID_INPUT_VALUE, "페이지 번호와 크기를 확인해 주세요.");
        }
        return transactionRunner.readOnly(() -> new BookmarkedPostListResult(
                page, size, port.countBookmarkedPosts(userId), port.findBookmarkedPosts(userId, page, size)));
    }
}
