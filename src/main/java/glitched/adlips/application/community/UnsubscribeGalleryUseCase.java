package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.GallerySubscriptionPort;
import glitched.adlips.application.port.TransactionRunner;

public class UnsubscribeGalleryUseCase {

    private final GallerySubscriptionPort port;
    private final TransactionRunner transactionRunner;

    public UnsubscribeGalleryUseCase(GallerySubscriptionPort port) {
        this(port, TransactionRunner.direct());
    }

    public UnsubscribeGalleryUseCase(GallerySubscriptionPort port, TransactionRunner transactionRunner) {
        this.port = port;
        this.transactionRunner = transactionRunner;
    }

    public void execute(Long userId, Long galleryId) {
        transactionRunner.required(() -> {
            if (!port.lockGallery(galleryId)) {
                throw new GalleryApplicationException(
                        GalleryErrorCode.GALLERY_NOT_FOUND, "존재하지 않거나 삭제된 갤러리입니다.");
            }
            if (!port.isSubscribed(userId, galleryId)) {
                throw new GalleryApplicationException(
                        GalleryErrorCode.NOT_SUBSCRIBED, "구독 중인 갤러리가 아닙니다.");
            }
            port.deleteSubscription(userId, galleryId);
            return null;
        });
    }
}
