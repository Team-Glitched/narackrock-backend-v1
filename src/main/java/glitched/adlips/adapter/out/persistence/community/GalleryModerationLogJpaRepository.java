package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.domain.community.GalleryModerationLog;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface GalleryModerationLogJpaRepository extends JpaRepository<GalleryModerationLog, Long> {

    @Query("""
            SELECT l FROM GalleryModerationLog l
            WHERE l.gallery.id = :galleryId
              AND (:cursor IS NULL OR l.id < :cursor)
            ORDER BY l.id DESC
            """)
    List<GalleryModerationLog> findPage(
            @Param("galleryId") Long galleryId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
