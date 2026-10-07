package glitched.adlips.application.community.port.out;

import glitched.adlips.domain.community.Gallery;
import glitched.adlips.domain.community.GalleryRole;
import java.util.Optional;

public interface GalleryManagementPort {
    Long createGallery(String name, String description);

    void addManager(Long galleryId, Long userId, GalleryRole role);

    Optional<Gallery> findGalleryForUpdate(Long galleryId);

    void updateGallery(Gallery gallery);
}
