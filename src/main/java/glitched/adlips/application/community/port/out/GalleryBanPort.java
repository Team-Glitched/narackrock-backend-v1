package glitched.adlips.application.community.port.out;

import glitched.adlips.domain.community.GalleryBan;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GalleryBanPort {
    boolean existsActive(Long galleryId, Long userId, LocalDateTime now);

    Long save(GalleryBan ban);

    Optional<GalleryBan> findById(Long banId);

    void update(GalleryBan ban);

    /** cursor가 null이면 처음부터. id 내림차순. activeOnly면 now 기준 유효한 제재만. */
    List<GalleryBanItem> findPage(Long galleryId, boolean activeOnly, LocalDateTime now, Long cursor, int limit);
}
