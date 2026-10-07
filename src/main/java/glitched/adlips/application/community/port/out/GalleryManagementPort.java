package glitched.adlips.application.community.port.out;

import glitched.adlips.domain.community.GalleryRole;

public interface GalleryManagementPort {
    Long createGallery(String name, String description);

    void addManager(Long galleryId, Long userId, GalleryRole role);
}
