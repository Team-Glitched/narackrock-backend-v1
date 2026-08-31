package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostQueryItem;
import java.util.List;

public record PostListResult(
        Long galleryId,
        String galleryName,
        PostSort currentSort,
        String searchedKeyword,
        long totalCount,
        List<PostQueryItem> posts
) {
    public PostListResult(Long galleryId, long totalCount, List<PostQueryItem> posts) {
        this(galleryId, null, PostSort.LATEST, null, totalCount, posts);
    }

    public PostListResult {
        posts = List.copyOf(posts);
    }
}
