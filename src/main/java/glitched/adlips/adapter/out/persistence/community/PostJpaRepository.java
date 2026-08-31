package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.domain.community.Post;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PostJpaRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Post> findActiveById(@Param("id") Long id);

    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.gallery.id = :galleryId AND p.deletedAt IS NULL")
    Optional<Post> findByIdAndGalleryIdAndDeletedAtIsNull(@Param("id") Long id, @Param("galleryId") Long galleryId);
}
