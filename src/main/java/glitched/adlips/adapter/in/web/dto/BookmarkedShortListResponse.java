package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.bookmark.BookmarkedShortPage;
import glitched.adlips.application.bookmark.port.out.BookmarkedShortItem;
import java.time.LocalDateTime;
import java.util.List;

public record BookmarkedShortListResponse(List<ShortItem> items, Page page) {

    public record ShortItem(
            Long shortId,
            String title,
            String albumImageUrl,
            String completionStatus,
            Author author,
            int likeCount,
            long commentCount,
            LocalDateTime bookmarkedAt
    ) {
    }

    public record Author(Long userId, String nickname, String profileImageUrl) {
    }

    public record Page(Long nextCursor, boolean hasMore, int size) {
    }

    public static BookmarkedShortListResponse from(BookmarkedShortPage page) {
        List<ShortItem> items = page.items().stream()
                .map(BookmarkedShortListResponse::toShortItem)
                .toList();
        return new BookmarkedShortListResponse(items, new Page(page.nextCursor(), page.hasMore(), page.size()));
    }

    private static ShortItem toShortItem(BookmarkedShortItem item) {
        return new ShortItem(
                item.shortId(),
                item.title(),
                item.albumImageUrl(),
                item.status(),
                new Author(item.authorId(), item.nickname(), item.profileImageUrl()),
                item.likeCount(),
                item.commentCount(),
                item.bookmarkedAt());
    }
}
