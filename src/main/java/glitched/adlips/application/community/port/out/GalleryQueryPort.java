package glitched.adlips.application.community.port.out;

import java.util.List;

public interface GalleryQueryPort {

    List<GalleryQueryItem> findGalleries();

    List<GalleryQueryItem> findManageableGalleries(Long userId);

    List<GalleryQueryItem> findSubscribedGalleries(Long userId);
}
