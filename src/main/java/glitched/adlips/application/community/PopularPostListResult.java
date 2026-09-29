package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostQueryItem;
import java.util.List;

public record PopularPostListResult(int page, int size, long totalCount, List<PostQueryItem> posts) {

    public PopularPostListResult {
        posts = List.copyOf(posts);
    }
}
