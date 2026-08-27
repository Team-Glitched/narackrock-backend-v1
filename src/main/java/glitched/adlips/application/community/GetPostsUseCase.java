package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostQueryItem;
import glitched.adlips.application.community.port.out.PostQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import java.util.List;

public class GetPostsUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final PostQueryPort port;
    private final TransactionRunner transactionRunner;

    public GetPostsUseCase(PostQueryPort port) {
        this(port, TransactionRunner.direct());
    }

    public GetPostsUseCase(PostQueryPort port, TransactionRunner transactionRunner) {
        this.port = port;
        this.transactionRunner = transactionRunner;
    }

    public PostListResult execute(Long galleryId, int page, int size) {
        return execute(galleryId, page, size, null, PostSort.LATEST.name());
    }

    public PostListResult execute(
            Long galleryId,
            int page,
            int size,
            String keyword,
            String sort
    ) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new PostApplicationException(PostErrorCode.VALIDATION_ERROR, "페이지 번호와 크기를 확인해 주세요.");
        }
        return transactionRunner.readOnly(() -> executeInternal(galleryId, page, size, keyword, sort));
    }

    private PostListResult executeInternal(
            Long galleryId,
            int page,
            int size,
            String keyword,
            String sort
    ) {
        String normalizedKeyword = normalizeKeyword(keyword);
        PostSort postSort = PostSort.from(sort);
        if (!port.existsGallery(galleryId)) {
            throw new PostApplicationException(PostErrorCode.GALLERY_NOT_FOUND, "존재하지 않는 갤러리입니다.");
        }
        String galleryName = port.findGalleryName(galleryId).orElse(null);
        List<PostQueryItem> posts = port.findPosts(galleryId, normalizedKeyword, postSort, page, size);
        long totalCount = port.countPosts(galleryId, normalizedKeyword);
        return new PostListResult(galleryId, galleryName, postSort, normalizedKeyword, totalCount, posts);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        if (keyword.isBlank()) {
            throw new PostApplicationException(
                    PostErrorCode.INVALID_SEARCH_KEYWORD,
                    "검색어는 공백을 제외한 최소 1자 이상 입력해야 합니다.");
        }
        return keyword.trim();
    }
}
