package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.community.GalleryActor;
import glitched.adlips.domain.community.GalleryModerationPolicy;
import java.time.Clock;
import java.time.LocalDateTime;

public class GetMyGalleryRoleUseCase {

    private final GalleryAuthorityPort authorityPort;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public GetMyGalleryRoleUseCase(GalleryAuthorityPort authorityPort, TransactionRunner transactionRunner, Clock clock) {
        this.authorityPort = authorityPort;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public GalleryRoleResult execute(Long userId, Long galleryId) {
        return transactionRunner.readOnly(() -> {
            GalleryModerationGuard.requireGallery(authorityPort, galleryId);
            GalleryActor actor = GalleryModerationGuard.requireActor(
                    authorityPort, userId, galleryId, LocalDateTime.now(clock));
            return new GalleryRoleResult(
                    galleryId, actor.galleryRole(), actor.isAdmin(), GalleryModerationPolicy.canManage(actor));
        });
    }
}
