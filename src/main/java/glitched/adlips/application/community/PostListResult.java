package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostQueryItem;
import java.util.List;

public record PostListResult(Long galleryId, long totalCount, List<PostQueryItem> posts) {
    public PostListResult {
        posts = List.copyOf(posts);
    }
}
