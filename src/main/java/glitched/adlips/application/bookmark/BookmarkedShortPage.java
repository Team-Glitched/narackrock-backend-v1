package glitched.adlips.application.bookmark;

import glitched.adlips.application.bookmark.port.out.BookmarkedShortItem;
import java.util.List;

public record BookmarkedShortPage(List<BookmarkedShortItem> items, Long nextCursor, boolean hasMore, int size) {

    public BookmarkedShortPage {
        items = List.copyOf(items);
    }
}
