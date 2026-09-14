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
            validateActiveShort(shortId);
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

    private void validateActiveShort(Long shortId) {
        if (!bookmarkPort.lockActiveShort(shortId)) {
            throw new ShortBookmarkApplicationException(
                    ShortBookmarkErrorCode.SHORT_NOT_FOUND,
                    "숏폼을 찾을 수 없습니다."
            );
        }
    }
}
