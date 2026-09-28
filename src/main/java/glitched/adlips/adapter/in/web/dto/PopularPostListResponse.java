package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.community.PopularPostListResult;
import glitched.adlips.application.community.port.out.PostQueryItem;
import java.time.LocalDateTime;
import java.util.List;

public record PopularPostListResponse(int page, int size, long totalCount, List<PostItem> posts) {

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
            LocalDateTime createdAt
    ) {
    }

    public record Gallery(Long galleryId, String name) {
    }

    public record Writer(Long userId, String nickname, String profileImageUrl) {
    }

    public static PopularPostListResponse from(PopularPostListResult result) {
        List<PostItem> items = result.posts().stream()
                .map(PopularPostListResponse::toPostItem)
                .toList();
        return new PopularPostListResponse(result.page(), result.size(), result.totalCount(), items);
    }

    private static PostItem toPostItem(PostQueryItem item) {
        return new PostItem(
                item.postId(),
                new Gallery(item.galleryId(), item.galleryName()),
                new Writer(item.userId(), item.nickname(), item.profileImageUrl()),
                item.title(),
                ContentPreview.of(item.content()),
                item.imageUrl(),
                item.viewCount(),
                item.likeCount(),
                item.commentCount(),
                item.createdAt());
    }
}
