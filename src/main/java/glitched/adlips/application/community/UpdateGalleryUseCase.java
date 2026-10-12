package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.community.port.out.GalleryManagementPort;
import glitched.adlips.application.community.port.out.GalleryModerationLogPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.community.Gallery;
import glitched.adlips.domain.community.GalleryModerationActionType;
import java.time.Clock;
import java.time.LocalDateTime;

public class UpdateGalleryUseCase {

    private final GalleryAuthorityPort authorityPort;
    private final GalleryManagementPort managementPort;
    private final GalleryModerationLogPort logPort;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public UpdateGalleryUseCase(
            GalleryAuthorityPort authorityPort,
            GalleryManagementPort managementPort,
            GalleryModerationLogPort logPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this.authorityPort = authorityPort;
        this.managementPort = managementPort;
        this.logPort = logPort;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public void execute(Long actorId, Long galleryId, String description) {
        transactionRunner.<Void>required(() -> {
            Gallery gallery = managementPort.findGalleryForUpdate(galleryId)
                    .orElseThrow(GalleryModerationGuard::galleryNotFound);
            GalleryModerationGuard.requireManager(authorityPort, actorId, galleryId, LocalDateTime.now(clock));
            try {
                gallery.changeDescription(description);
            } catch (IllegalArgumentException e) {
                throw new GalleryModerationApplicationException(
                        GalleryModerationErrorCode.VALIDATION_ERROR, e.getMessage());
            }
            managementPort.updateGallery(gallery);
            logPort.save(galleryId, actorId, GalleryModerationActionType.UPDATE_GALLERY, null, galleryId, null);
            return null;
        });
    }
}
