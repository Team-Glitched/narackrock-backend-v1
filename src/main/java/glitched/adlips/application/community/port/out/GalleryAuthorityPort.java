package glitched.adlips.application.community.port.out;

import glitched.adlips.domain.community.GalleryActor;
import java.time.LocalDateTime;
import java.util.Optional;

public interface GalleryAuthorityPort {
    boolean existsGallery(Long galleryId);

    /** 활성 사용자가 아니면 빈 값. 서비스 전역 정지 여부는 now 기준으로 판정한다. */
    Optional<GalleryActor> findActor(Long userId, Long galleryId, LocalDateTime now);
}
