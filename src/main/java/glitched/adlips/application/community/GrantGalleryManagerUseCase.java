package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.community.port.out.GalleryBanPort;
import glitched.adlips.application.community.port.out.GalleryManagementPort;
import glitched.adlips.application.community.port.out.GalleryModerationLogPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.community.GalleryActor;
import glitched.adlips.domain.community.GalleryModerationActionType;
import glitched.adlips.domain.community.GalleryRole;
import java.time.Clock;
import java.time.LocalDateTime;

/** 커뮤니티 어드민(MANAGER)을 지정한다. 해당 커뮤니티의 어드민과 운영자 어드민이 할 수 있다. */
public class GrantGalleryManagerUseCase {

    private final GalleryAuthorityPort authorityPort;
    private final GalleryManagementPort managementPort;
    private final GalleryBanPort banPort;
    private final GalleryModerationLogPort logPort;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public GrantGalleryManagerUseCase(
            GalleryAuthorityPort authorityPort,
            GalleryManagementPort managementPort,
            GalleryBanPort banPort,
            GalleryModerationLogPort logPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this.authorityPort = authorityPort;
        this.managementPort = managementPort;
        this.banPort = banPort;
        this.logPort = logPort;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public void execute(Long actorId, Long galleryId, Long targetUserId) {
        transactionRunner.<Void>required(() -> {
            LocalDateTime now = LocalDateTime.now(clock);
            managementPort.findGalleryForUpdate(galleryId).orElseThrow(GalleryModerationGuard::galleryNotFound);
            GalleryModerationGuard.requireManager(authorityPort, actorId, galleryId, now);
            if (targetUserId == null) {
                throw new GalleryModerationApplicationException(
                        GalleryModerationErrorCode.VALIDATION_ERROR, "지정할 사용자를 선택해 주세요.");
            }
            GalleryActor target = authorityPort.findActor(targetUserId, galleryId, now)
                    .orElseThrow(() -> new GalleryModerationApplicationException(
                            GalleryModerationErrorCode.TARGET_USER_NOT_FOUND, "지정할 사용자를 찾을 수 없습니다."));
            if (target.isManager()) {
                throw new GalleryModerationApplicationException(
                        GalleryModerationErrorCode.ALREADY_GALLERY_MANAGER, "이미 이 커뮤니티의 커뮤니티 어드민입니다.");
            }
            if (target.globallyBanned() || banPort.existsActive(galleryId, targetUserId, now)) {
                throw new GalleryModerationApplicationException(
                        GalleryModerationErrorCode.CANNOT_GRANT_MANAGER,
                        "이용 제한 중인 사용자는 커뮤니티 어드민으로 지정할 수 없습니다.");
            }

            managementPort.addManager(galleryId, targetUserId, GalleryRole.MANAGER);
            logPort.save(galleryId, actorId, GalleryModerationActionType.GRANT_MANAGER, targetUserId, null, null);
            return null;
        });
    }
}
