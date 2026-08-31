package glitched.adlips.application.community.port.out;

import glitched.adlips.domain.community.Post;
import java.util.Optional;

public interface PostPort {
    boolean existsGallery(Long galleryId);

    Long save(Long galleryId, Long userId, String title, String content);

    Optional<Post> findActivePost(Long postId);

    default Optional<Post> findActivePost(Long postId, Long galleryId) {
        return findActivePost(postId).filter(post -> post.getGalleryId().equals(galleryId));
    }

    void update(Post post);
}
