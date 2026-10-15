package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.GalleryBanQueryPort;
import glitched.adlips.application.community.port.out.GallerySubscriptionPort;
import glitched.adlips.application.port.TransactionRunner;
import java.time.Clock;
import java.time.LocalDateTime;

public class SubscribeGalleryUseCase {

    private final GallerySubscriptionPort port;
    private final GalleryBanQueryPort galleryBanQueryPort;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public SubscribeGalleryUseCase(GallerySubscriptionPort port) {
        this(port, TransactionRunner.direct());
    }

    public SubscribeGalleryUseCase(GallerySubscriptionPort port, TransactionRunner transactionRunner) {
        this(port, GalleryBanQueryPort.none(), transactionRunner, Clock.systemUTC());
    }

    public SubscribeGalleryUseCase(
            GallerySubscriptionPort port,
            GalleryBanQueryPort galleryBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this.port = port;
        this.galleryBanQueryPort = galleryBanQueryPort;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public void execute(Long userId, Long galleryId) {
        transactionRunner.required(() -> {
            if (!port.lockGallery(galleryId)) {
                throw new GalleryApplicationException(
                        GalleryErrorCode.GALLERY_NOT_FOUND, "존재하지 않거나 삭제된 갤러리입니다.");
            }
            if (galleryBanQueryPort.isExpelled(galleryId, userId, LocalDateTime.now(clock))) {
                throw new GalleryApplicationException(
                        GalleryErrorCode.EXPELLED_FROM_GALLERY, "이 갤러리에서 추방되어 구독할 수 없습니다.");
            }
            if (port.isSubscribed(userId, galleryId)) {
                throw new GalleryApplicationException(
                        GalleryErrorCode.ALREADY_SUBSCRIBED, "이미 구독 중인 갤러리입니다.");
            }
            port.saveSubscription(userId, galleryId);
            return null;
        });
    }
}
