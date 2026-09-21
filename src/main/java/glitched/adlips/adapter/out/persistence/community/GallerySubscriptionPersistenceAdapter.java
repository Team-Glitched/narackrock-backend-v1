package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.application.community.port.out.GallerySubscriptionPort;
import org.springframework.stereotype.Repository;

@Repository
public class GallerySubscriptionPersistenceAdapter implements GallerySubscriptionPort {

    private final GalleryJpaRepository galleryRepository;
    private final GallerySubscriptionJpaRepository subscriptionRepository;

    public GallerySubscriptionPersistenceAdapter(
            GalleryJpaRepository galleryRepository,
            GallerySubscriptionJpaRepository subscriptionRepository
    ) {
        this.galleryRepository = galleryRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public boolean lockGallery(Long galleryId) {
        return galleryRepository.findByIdForUpdate(galleryId).isPresent();
    }

    @Override
    public boolean isSubscribed(Long userId, Long galleryId) {
        return subscriptionRepository.existsByUser_IdAndGallery_Id(userId, galleryId);
    }

    @Override
    public void saveSubscription(Long userId, Long galleryId) {
        subscriptionRepository.insertSubscription(userId, galleryId);
    }

    @Override
    public void deleteSubscription(Long userId, Long galleryId) {
        subscriptionRepository.deleteSubscription(userId, galleryId);
    }
}
