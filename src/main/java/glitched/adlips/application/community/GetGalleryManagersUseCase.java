package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.community.port.out.GalleryManagementPort;
import glitched.adlips.application.community.port.out.GalleryManagerItem;
import glitched.adlips.application.port.TransactionRunner;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

public class GetGalleryManagersUseCase {

    private final GalleryAuthorityPort authorityPort;
    private final GalleryManagementPort managementPort;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public GetGalleryManagersUseCase(
            GalleryAuthorityPort authorityPort,
            GalleryManagementPort managementPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this.authorityPort = authorityPort;
        this.managementPort = managementPort;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public List<GalleryManagerItem> execute(Long actorId, Long galleryId) {
        return transactionRunner.readOnly(() -> {
            LocalDateTime now = LocalDateTime.now(clock);
            GalleryModerationGuard.requireGallery(authorityPort, galleryId);
            GalleryModerationGuard.requireManager(authorityPort, actorId, galleryId, now);
            return managementPort.findManagers(galleryId);
        });
    }
}
