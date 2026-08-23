package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostQueryItem;
import glitched.adlips.application.community.port.out.PostQueryPort;
import java.util.List;

public class GetPostsUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final PostQueryPort port;

    public GetPostsUseCase(PostQueryPort port) {
        this.port = port;
    }

    public PostListResult execute(Long galleryId, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new PostApplicationException(PostErrorCode.VALIDATION_ERROR, "페이지 번호와 크기를 확인해 주세요.");
        }
        if (!port.existsGallery(galleryId)) {
            throw new PostApplicationException(PostErrorCode.GALLERY_NOT_FOUND, "존재하지 않는 갤러리입니다.");
        }
        List<PostQueryItem> posts = port.findPosts(galleryId, page, size);
        long totalCount = port.countPosts(galleryId);
        return new PostListResult(galleryId, totalCount, posts);
    }
}
