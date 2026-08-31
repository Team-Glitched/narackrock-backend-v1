package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.community.PostListResult;
import glitched.adlips.application.community.PostSort;
import glitched.adlips.application.community.port.out.PostQueryItem;
import java.time.LocalDateTime;
import java.util.List;

public record PostListResponse(
        Long galleryId,
        String galleryName,
        PostSort currentSort,
        String searchedKeyword,
        long totalCount,
        List<PostItem> posts
) {

    public record PostItem(
            Long postId,
            Writer writer,
            String title,
            int viewCount,
            int likeCount,
            int commentCount,
            LocalDateTime createdAt
    ) {
    }

    public record Writer(Long userId, String nickname, String profileImageUrl) {
    }

    public static PostListResponse from(PostListResult result) {
        List<PostItem> items = result.posts().stream()
                .map(PostListResponse::toPostItem)
                .toList();
        return new PostListResponse(
                result.galleryId(), result.galleryName(), result.currentSort(), result.searchedKeyword(),
                result.totalCount(), items);
    }

    private static PostItem toPostItem(PostQueryItem item) {
        return new PostItem(
                item.postId(),
                new Writer(item.userId(), item.nickname(), item.profileImageUrl()),
                item.title(),
                item.viewCount(),
                item.likeCount(),
                item.commentCount(),
                item.createdAt());
    }
}
