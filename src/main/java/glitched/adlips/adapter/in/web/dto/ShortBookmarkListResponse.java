package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.bookmark.BookmarkedShortPage;
import java.util.List;

public record ShortBookmarkListResponse(List<ShortItem> content, Long nextCursor, boolean hasNext) {

    public record ShortItem(Long shortId, String title, String albumImageUrl) {
    }

    public static ShortBookmarkListResponse from(BookmarkedShortPage page) {
        List<ShortItem> content = page.items().stream()
                .map(item -> new ShortItem(item.shortId(), item.title(), item.albumImageUrl()))
                .toList();
        return new ShortBookmarkListResponse(content, page.nextCursor(), page.hasMore());
    }
}
