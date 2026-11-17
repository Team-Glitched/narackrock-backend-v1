package glitched.adlips.application.community.port.out;

import glitched.adlips.domain.community.Gallery;
import glitched.adlips.domain.community.GalleryRole;
import java.util.List;
import java.util.Optional;

public interface GalleryManagementPort {
    Long createGallery(String name, String description);

    void addManager(Long galleryId, Long userId, GalleryRole role);

    Optional<Gallery> findGalleryForUpdate(Long galleryId);

    void updateGallery(Gallery gallery);

    /** 탈퇴하지 않은 커뮤니티 어드민(MANAGER). 지정된 순서. */
    List<GalleryManagerItem> findManagers(Long galleryId);

    /** 삭제했으면 true. */
    boolean removeManager(Long galleryId, Long userId);

    /** 탈퇴하지 않은 커뮤니티 어드민(MANAGER) 수. */
    long countActiveManagers(Long galleryId);
}
