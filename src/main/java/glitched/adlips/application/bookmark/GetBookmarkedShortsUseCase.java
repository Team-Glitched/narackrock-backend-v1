package glitched.adlips.application.bookmark;

import glitched.adlips.application.bookmark.port.out.BookmarkQueryPort;
import glitched.adlips.application.bookmark.port.out.BookmarkedShortItem;
import glitched.adlips.application.port.TransactionRunner;
import java.util.List;

/** 숏폼 피드와 같은 커서 방식이다. 커서는 숏폼 id가 아니라 북마크 id다. */
public class GetBookmarkedShortsUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final BookmarkQueryPort port;
    private final TransactionRunner transactionRunner;

    public GetBookmarkedShortsUseCase(BookmarkQueryPort port, TransactionRunner transactionRunner) {
        this.port = port;
        this.transactionRunner = transactionRunner;
    }

    public BookmarkedShortPage execute(Long userId, Long cursor, int size) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BookmarkApplicationException(BookmarkErrorCode.INVALID_INPUT_VALUE, "페이지 크기를 확인해 주세요.");
        }
        List<BookmarkedShortItem> fetched = transactionRunner.readOnly(
                () -> port.findBookmarkedShorts(userId, cursor, size + 1));
        boolean hasMore = fetched.size() > size;
        List<BookmarkedShortItem> items = hasMore ? fetched.subList(0, size) : fetched;
        Long nextCursor = items.isEmpty() ? null : items.getLast().bookmarkId();
        return new BookmarkedShortPage(items, nextCursor, hasMore, size);
    }
}
