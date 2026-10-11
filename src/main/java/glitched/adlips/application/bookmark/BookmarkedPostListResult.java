package glitched.adlips.application.bookmark;

import glitched.adlips.application.bookmark.port.out.BookmarkedPostItem;
import java.util.List;

public record BookmarkedPostListResult(int page, int size, long totalCount, List<BookmarkedPostItem> posts) {

    public BookmarkedPostListResult {
        posts = List.copyOf(posts);
    }
}
