package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostBookmarkPort;
import glitched.adlips.application.port.TransactionRunner;

public class TogglePostBookmarkUseCase {

    private final PostBookmarkPort bookmarkPort;
    private final TransactionRunner transactionRunner;

    public TogglePostBookmarkUseCase(PostBookmarkPort bookmarkPort, TransactionRunner transactionRunner) {
        this.bookmarkPort = bookmarkPort;
        this.transactionRunner = transactionRunner;
    }

    public PostBookmarkResult toggle(Long userId, Long postId) {
        return transactionRunner.required(() -> {
            if (!bookmarkPort.lockActivePost(postId)) {
                throw new PostApplicationException(PostErrorCode.POST_NOT_FOUND, "존재하지 않거나 삭제된 게시글입니다.");
            }
            if (bookmarkPort.isBookmarked(userId, postId)) {
                bookmarkPort.deleteBookmark(userId, postId);
                return new PostBookmarkResult(postId, false);
            }
            bookmarkPort.saveBookmark(userId, postId);
            return new PostBookmarkResult(postId, true);
        });
    }
}
