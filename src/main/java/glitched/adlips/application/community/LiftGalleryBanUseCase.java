package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.community.port.out.GalleryBanPort;
import glitched.adlips.application.community.port.out.GalleryManagementPort;
import glitched.adlips.application.community.port.out.GalleryModerationLogPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.community.GalleryBan;
import glitched.adlips.domain.community.GalleryModerationActionType;
import java.time.Clock;
import java.time.LocalDateTime;

public class LiftGalleryBanUseCase {

    private final GalleryAuthorityPort authorityPort;
    private final GalleryManagementPort managementPort;
    private final GalleryBanPort banPort;
    private final UserRepositoryPort userRepository;
    private final GalleryModerationLogPort logPort;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public LiftGalleryBanUseCase(
            GalleryAuthorityPort authorityPort,
            GalleryManagementPort managementPort,
            GalleryBanPort banPort,
            UserRepositoryPort userRepository,
            GalleryModerationLogPort logPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this.authorityPort = authorityPort;
        this.managementPort = managementPort;
        this.banPort = banPort;
        this.userRepository = userRepository;
        this.logPort = logPort;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public void execute(Long actorId, Long galleryId, Long banId) {
        transactionRunner.<Void>required(() -> {
            LocalDateTime now = LocalDateTime.now(clock);
            managementPort.findGalleryForUpdate(galleryId).orElseThrow(GalleryModerationGuard::galleryNotFound);
            GalleryModerationGuard.requireManager(authorityPort, actorId, galleryId, now);
            GalleryBan ban = banPort.findById(banId)
                    .filter(found -> found.getGalleryId().equals(galleryId))
                    .filter(found -> found.isActiveAt(now))
                    .orElseThrow(() -> new GalleryModerationApplicationException(
                            GalleryModerationErrorCode.GALLERY_BAN_NOT_FOUND, "해제할 수 있는 제재가 없습니다."));

            ban.lift(userRepository.findById(actorId).orElseThrow(), now);
            banPort.update(ban);
            logPort.save(galleryId, actorId, GalleryModerationActionType.LIFT_BAN, ban.getUserId(), banId, null);
            return null;
        });
    }
}
