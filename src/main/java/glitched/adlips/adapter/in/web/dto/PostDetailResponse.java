package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.community.port.out.PostQueryItem;
import java.time.LocalDateTime;

public record PostDetailResponse(
        Long postId,
        Gallery gallery,
        Writer writer,
        String title,
        String content,
        String imageUrl,
        int viewCount,
        int likeCount,
        int dislikeCount,
        int commentCount,
        boolean isLiked,
        boolean isDisliked,
        boolean isBookmarked,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record Gallery(Long galleryId, String name) {
    }

    public record Writer(Long userId, String nickname, String profileImageUrl) {
    }

    public static PostDetailResponse from(PostQueryItem item) {
        return new PostDetailResponse(
                item.postId(),
                new Gallery(item.galleryId(), item.galleryName()),
                new Writer(item.userId(), item.nickname(), item.profileImageUrl()),
                item.title(),
                item.content(),
                item.imageUrl(),
                item.viewCount(),
                item.likeCount(),
                item.dislikeCount(),
                item.commentCount(),
                item.liked(),
                item.disliked(),
                item.bookmarked(),
                item.createdAt(),
                item.updatedAt());
    }
}
