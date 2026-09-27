package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.bookmark.BookmarkedPostListResult;
import glitched.adlips.application.bookmark.port.out.BookmarkedPostItem;
import java.time.LocalDateTime;
import java.util.List;

public record BookmarkedPostListResponse(int page, int size, long totalCount, List<PostItem> posts) {

    public record PostItem(
            Long postId,
            Gallery gallery,
            Writer writer,
            String title,
            String contentPreview,
            String imageUrl,
            int viewCount,
            int likeCount,
            int commentCount,
            LocalDateTime createdAt,
            LocalDateTime bookmarkedAt
    ) {
    }

    public record Gallery(Long galleryId, String name) {
    }

    public record Writer(Long userId, String nickname, String profileImageUrl) {
    }

    public static BookmarkedPostListResponse from(BookmarkedPostListResult result) {
        List<PostItem> items = result.posts().stream()
                .map(BookmarkedPostListResponse::toPostItem)
                .toList();
        return new BookmarkedPostListResponse(result.page(), result.size(), result.totalCount(), items);
    }

    private static PostItem toPostItem(BookmarkedPostItem item) {
        return new PostItem(
                item.postId(),
                new Gallery(item.galleryId(), item.galleryName()),
                new Writer(item.writerId(), item.nickname(), item.profileImageUrl()),
                item.title(),
                ContentPreview.of(item.content()),
                item.imageUrl(),
                item.viewCount(),
                item.likeCount(),
                item.commentCount(),
                item.createdAt(),
                item.bookmarkedAt());
    }
}
