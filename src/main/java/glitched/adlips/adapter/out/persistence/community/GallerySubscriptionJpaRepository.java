package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.domain.community.GallerySubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface GallerySubscriptionJpaRepository extends JpaRepository<GallerySubscription, Long> {

    boolean existsByUser_IdAndGallery_Id(Long userId, Long galleryId);

    // ShortBookmarkJpaRepository와 같은 방식 — 엔티티를 조립하지 않고 바로 쓴다.
    @Modifying
    @Query(
            value = "INSERT INTO gallery_subs (user_id, gallery_id, created_at)"
                    + " VALUES (:userId, :galleryId, CURRENT_TIMESTAMP)",
            nativeQuery = true
    )
    void insertSubscription(@Param("userId") Long userId, @Param("galleryId") Long galleryId);

    @Modifying
    @Query(
            value = "DELETE FROM gallery_subs WHERE user_id = :userId AND gallery_id = :galleryId",
            nativeQuery = true
    )
    void deleteSubscription(@Param("userId") Long userId, @Param("galleryId") Long galleryId);
}
