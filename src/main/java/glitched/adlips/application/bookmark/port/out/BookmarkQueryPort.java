package glitched.adlips.application.bookmark.port.out;

import java.util.List;

public interface BookmarkQueryPort {
    List<BookmarkedPostItem> findBookmarkedPosts(Long userId, int page, int size);
    long countBookmarkedPosts(Long userId);
    List<BookmarkedShortItem> findBookmarkedShorts(Long userId, Long cursor, int limit);
}
