package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.community.port.out.GalleryBanItem;
import glitched.adlips.application.community.port.out.GalleryBanPort;
import glitched.adlips.application.port.TransactionRunner;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

public class GetGalleryBansUseCase {

    private final GalleryAuthorityPort authorityPort;
    private final GalleryBanPort banPort;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public GetGalleryBansUseCase(
            GalleryAuthorityPort authorityPort,
            GalleryBanPort banPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this.authorityPort = authorityPort;
        this.banPort = banPort;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public GalleryBanPage execute(Long actorId, Long galleryId, boolean activeOnly, Long cursor, int size) {
        return transactionRunner.readOnly(() -> {
            LocalDateTime now = LocalDateTime.now(clock);
            GalleryModerationGuard.requireGallery(authorityPort, galleryId);
            GalleryModerationGuard.requireManager(authorityPort, actorId, galleryId, now);
            GalleryModerationGuard.validatePage(cursor, size);

            List<GalleryBanItem> queried = banPort.findPage(galleryId, activeOnly, now, cursor, size + 1);
            boolean hasNext = queried.size() > size;
            List<GalleryBanItem> content = hasNext ? queried.subList(0, size) : queried;
            return new GalleryBanPage(List.copyOf(content), hasNext ? content.getLast().banId() : null, hasNext);
        });
    }
}
