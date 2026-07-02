package glitched.adlips.application.shorts;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.shorts.port.out.ShortBookmarkPort;

public class ToggleShortBookmarkUseCase {

    private final ShortBookmarkPort bookmarkPort;
    private final TransactionRunner transactionRunner;

    public ToggleShortBookmarkUseCase(ShortBookmarkPort bookmarkPort, TransactionRunner transactionRunner) {
        this.bookmarkPort = bookmarkPort;
        this.transactionRunner = transactionRunner;
    }

    public ShortBookmarkResult toggle(Long userId, Long shortId) {
        return transactionRunner.required(() -> {
            boolean current = bookmarkPort.isBookmarked(userId, shortId);
            if (current) {
                bookmarkPort.deleteBookmark(userId, shortId);
                return new ShortBookmarkResult(shortId, false);
            } else {
                bookmarkPort.saveBookmark(userId, shortId);
                return new ShortBookmarkResult(shortId, true);
            }
        });
    }
}
