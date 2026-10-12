package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.domain.community.GalleryBan;
import glitched.adlips.domain.community.GalleryBanType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface GalleryBanJpaRepository extends JpaRepository<GalleryBan, Long> {

    String ACTIVE = "b.liftedAt IS NULL AND (b.bannedUntil IS NULL OR b.bannedUntil > :now)";

    @Query("SELECT COUNT(b) > 0 FROM GalleryBan b"
            + " WHERE b.gallery.id = :galleryId AND b.user.id = :userId AND " + ACTIVE)
    boolean existsActive(
            @Param("galleryId") Long galleryId, @Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(b) > 0 FROM GalleryBan b"
            + " WHERE b.gallery.id = :galleryId AND b.user.id = :userId AND b.banType = :banType AND " + ACTIVE)
    boolean existsActiveOfType(
            @Param("galleryId") Long galleryId,
            @Param("userId") Long userId,
            @Param("banType") GalleryBanType banType,
            @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(b) > 0 FROM GalleryBan b, Post p"
            + " WHERE p.id = :postId AND b.gallery = p.gallery AND b.user.id = :userId AND " + ACTIVE)
    boolean existsActiveForPost(
            @Param("postId") Long postId, @Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(b) > 0 FROM GalleryBan b, PostComment c"
            + " WHERE c.id = :commentId AND b.gallery = c.post.gallery AND b.user.id = :userId AND " + ACTIVE)
    boolean existsActiveForComment(
            @Param("commentId") Long commentId, @Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT b FROM GalleryBan b"
            + " WHERE b.gallery.id = :galleryId"
            + " AND (:cursor IS NULL OR b.id < :cursor)"
            + " AND (:activeOnly = false OR (" + ACTIVE + "))"
            + " ORDER BY b.id DESC")
    List<GalleryBan> findPage(
            @Param("galleryId") Long galleryId,
            @Param("activeOnly") boolean activeOnly,
            @Param("now") LocalDateTime now,
            @Param("cursor") Long cursor,
            Pageable pageable);
}
