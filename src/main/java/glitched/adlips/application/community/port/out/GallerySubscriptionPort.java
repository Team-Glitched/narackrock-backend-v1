package glitched.adlips.application.community.port.out;

public interface GallerySubscriptionPort {

    /**
     * 갤러리 행을 비관적 락으로 잡아 둔다. 같은 사용자의 구독 요청이 동시에 들어와도
     * (user_id, gallery_id) 유니크 제약 위반이 나지 않도록 존재 확인과 락을 겸한다.
     *
     * @return 갤러리가 존재하면 true
     */
    boolean lockGallery(Long galleryId);

    boolean isSubscribed(Long userId, Long galleryId);

    void saveSubscription(Long userId, Long galleryId);

    void deleteSubscription(Long userId, Long galleryId);
}
