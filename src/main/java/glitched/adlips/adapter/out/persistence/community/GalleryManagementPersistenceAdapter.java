package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.application.community.port.out.GalleryManagementPort;
import glitched.adlips.domain.community.Gallery;
import glitched.adlips.domain.community.GalleryManager;
import glitched.adlips.domain.community.GalleryRole;
import glitched.adlips.domain.user.User;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class GalleryManagementPersistenceAdapter implements GalleryManagementPort {

    private final GalleryJpaRepository galleryRepository;
    private final GalleryManagerJpaRepository managerRepository;
    private final EntityManager entityManager;

    public GalleryManagementPersistenceAdapter(
            GalleryJpaRepository galleryRepository,
            GalleryManagerJpaRepository managerRepository,
            EntityManager entityManager
    ) {
        this.galleryRepository = galleryRepository;
        this.managerRepository = managerRepository;
        this.entityManager = entityManager;
    }

    @Override
    public Long createGallery(String name, String description) {
        return galleryRepository.save(Gallery.create(name, description)).getId();
    }

    @Override
    public void addManager(Long galleryId, Long userId, GalleryRole role) {
        managerRepository.save(GalleryManager.assign(
                entityManager.getReference(Gallery.class, galleryId),
                entityManager.getReference(User.class, userId),
                role));
    }

    @Override
    public Optional<Gallery> findGalleryForUpdate(Long galleryId) {
        return galleryRepository.findByIdForUpdate(galleryId);
    }

    @Override
    public void updateGallery(Gallery gallery) {
        galleryRepository.save(gallery);
    }
}
