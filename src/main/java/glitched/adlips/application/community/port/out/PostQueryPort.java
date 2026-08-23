package glitched.adlips.application.community.port.out;

import java.util.List;
import java.util.Optional;

public interface PostQueryPort {
    boolean existsGallery(Long galleryId);

    Optional<PostQueryItem> findDetail(Long postId, Long galleryId);

    List<PostQueryItem> findPosts(Long galleryId, int page, int size);

    long countPosts(Long galleryId);
}
