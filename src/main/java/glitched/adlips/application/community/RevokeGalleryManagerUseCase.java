package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.community.port.out.GalleryManagementPort;
import glitched.adlips.application.community.port.out.GalleryModerationLogPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.community.GalleryActor;
import glitched.adlips.domain.community.GalleryModerationActionType;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 커뮤니티 어드민(MANAGER)을 해임한다. 커뮤니티 어드민은 자신을 포함해 같은 커뮤니티의 어드민을 해임할 수 있지만,
 * 마지막 1명은 운영자 어드민만 해임할 수 있다.
 */
public class RevokeGalleryManagerUseCase {

    private final GalleryAuthorityPort authorityPort;
    private final GalleryManagementPort managementPort;
    private final GalleryModerationLogPort logPort;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public RevokeGalleryManagerUseCase(
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

    public void execute(Long actorId, Long galleryId, Long targetUserId) {
        transactionRunner.<Void>required(() -> {
            LocalDateTime now = LocalDateTime.now(clock);
            managementPort.findGalleryForUpdate(galleryId).orElseThrow(GalleryModerationGuard::galleryNotFound);
            GalleryActor actor = GalleryModerationGuard.requireManager(authorityPort, actorId, galleryId, now);
            authorityPort.findActor(targetUserId, galleryId, now)
                    .filter(GalleryActor::isManager)
                    .orElseThrow(() -> new GalleryModerationApplicationException(
                            GalleryModerationErrorCode.GALLERY_MANAGER_NOT_FOUND, "해임할 커뮤니티 어드민을 찾을 수 없습니다."));
            if (!actor.isAdmin() && managementPort.countActiveManagers(galleryId) <= 1) {
                throw new GalleryModerationApplicationException(
                        GalleryModerationErrorCode.LAST_GALLERY_MANAGER,
                        "마지막 커뮤니티 어드민은 운영자 어드민만 해임할 수 있습니다.");
            }

            managementPort.removeManager(galleryId, targetUserId);
            logPort.save(galleryId, actorId, GalleryModerationActionType.REVOKE_MANAGER, targetUserId, null, null);
            return null;
        });
    }
}
