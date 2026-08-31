package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.application.community.port.out.PostPort;
import glitched.adlips.domain.community.Gallery;
import glitched.adlips.domain.community.Post;
import glitched.adlips.domain.user.User;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PostPersistenceAdapter implements PostPort {

    private final GalleryJpaRepository galleryRepository;
    private final PostJpaRepository postRepository;
    private final EntityManager entityManager;

    public PostPersistenceAdapter(
            GalleryJpaRepository galleryRepository,
            PostJpaRepository postRepository,
            EntityManager entityManager
    ) {
        this.galleryRepository = galleryRepository;
        this.postRepository = postRepository;
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsGallery(Long galleryId) {
        return galleryRepository.existsById(galleryId);
    }

    @Override
    public Long save(Long galleryId, Long userId, String title, String content) {
        Gallery gallery = entityManager.getReference(Gallery.class, galleryId);
        User user = entityManager.getReference(User.class, userId);
        Post saved = postRepository.save(new Post(gallery, user, title, content));
        return saved.getId();
    }

    @Override
    public Optional<Post> findActivePost(Long postId) {
        return postRepository.findActiveById(postId);
    }

    public Optional<Post> findActivePost(Long postId, Long galleryId) {
        return postRepository.findByIdAndGalleryIdAndDeletedAtIsNull(postId, galleryId);
    }

    @Override
    public void update(Post post) {
        postRepository.save(post);
    }
}
