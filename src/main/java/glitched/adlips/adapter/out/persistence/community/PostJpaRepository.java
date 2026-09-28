package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.application.community.port.out.ReactionCounts;
import glitched.adlips.domain.community.Post;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PostJpaRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Post> findActiveById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Post> findActiveByIdForUpdate(@Param("id") Long id);

    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.gallery.id = :galleryId AND p.deletedAt IS NULL")
    Optional<Post> findByIdAndGalleryIdAndDeletedAtIsNull(@Param("id") Long id, @Param("galleryId") Long galleryId);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + :delta WHERE p.id = :id")
    void adjustCommentCount(@Param("id") Long id, @Param("delta") int delta);

    @Modifying
    @Query("""
            UPDATE Post p
               SET p.likeCount = p.likeCount + :likeDelta,
                   p.dislikeCount = p.dislikeCount + :dislikeDelta
             WHERE p.id = :id
            """)
    void adjustReactionCounts(
            @Param("id") Long id, @Param("likeDelta") int likeDelta, @Param("dislikeDelta") int dislikeDelta);

    @Query("""
            SELECT new glitched.adlips.application.community.port.out.ReactionCounts(p.likeCount, p.dislikeCount)
              FROM Post p
             WHERE p.id = :id
            """)
    Optional<ReactionCounts> findReactionCounts(@Param("id") Long id);
}
