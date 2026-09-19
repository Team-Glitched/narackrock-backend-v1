package glitched.adlips.application.community.port.out;

import java.util.List;

public interface GalleryQueryPort {

    List<GalleryQueryItem> findGalleries();

    List<GalleryQueryItem> findSubscribedGalleries(Long userId);
}
