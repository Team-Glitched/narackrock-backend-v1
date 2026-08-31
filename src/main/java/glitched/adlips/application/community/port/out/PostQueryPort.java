package glitched.adlips.application.community.port.out;

import glitched.adlips.application.community.PostSort;
import java.util.List;
import java.util.Optional;

public interface PostQueryPort {
    boolean existsGallery(Long galleryId);

    Optional<PostQueryItem> findDetail(Long postId, Long viewerId);

    List<PostQueryItem> findPosts(Long galleryId, String keyword, PostSort sort, int page, int size);

    long countPosts(Long galleryId, String keyword);

    Optional<String> findGalleryName(Long galleryId);

    default Optional<PostQueryItem> findDetail(Long postId, Long galleryId, boolean ignored) {
        return findDetail(postId, null);
    }

    default List<PostQueryItem> findPosts(Long galleryId, int page, int size) {
        return findPosts(galleryId, null, PostSort.LATEST, page, size);
    }

    default long countPosts(Long galleryId) {
        return countPosts(galleryId, null);
    }
}
