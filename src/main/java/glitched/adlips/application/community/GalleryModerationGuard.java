package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.domain.community.GalleryActor;
import glitched.adlips.domain.community.GalleryModerationPolicy;
import java.time.LocalDateTime;

final class GalleryModerationGuard {

    private GalleryModerationGuard() {
    }

    static void requireGallery(GalleryAuthorityPort authorityPort, Long galleryId) {
        if (!authorityPort.existsGallery(galleryId)) {
            throw galleryNotFound();
        }
    }

    static GalleryActor requireActor(GalleryAuthorityPort authorityPort, Long userId, Long galleryId, LocalDateTime now) {
        return authorityPort.findActor(userId, galleryId, now).orElseThrow(GalleryModerationGuard::forbidden);
    }

    static GalleryActor requireManager(
            GalleryAuthorityPort authorityPort, Long userId, Long galleryId, LocalDateTime now
    ) {
        GalleryActor actor = requireActor(authorityPort, userId, galleryId, now);
        if (!GalleryModerationPolicy.canManage(actor)) {
            throw forbidden();
        }
        return actor;
    }

    static GalleryModerationApplicationException galleryNotFound() {
        return new GalleryModerationApplicationException(
                GalleryModerationErrorCode.GALLERY_NOT_FOUND, "존재하지 않거나 삭제된 갤러리입니다.");
    }

    static GalleryModerationApplicationException forbidden() {
        return new GalleryModerationApplicationException(
                GalleryModerationErrorCode.FORBIDDEN, "갤러리 매니저 또는 관리자만 할 수 있습니다.");
    }

    static void validatePage(Long cursor, int size) {
        if ((cursor != null && cursor < 1) || size < 1 || size > 100) {
            throw new GalleryModerationApplicationException(
                    GalleryModerationErrorCode.VALIDATION_ERROR, "cursor와 size 값을 확인해 주세요.");
        }
    }
}
