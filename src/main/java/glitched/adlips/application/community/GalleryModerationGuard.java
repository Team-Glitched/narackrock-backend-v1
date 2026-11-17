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

    static void requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new GalleryModerationApplicationException(
                    GalleryModerationErrorCode.VALIDATION_ERROR, "삭제 사유를 입력해 주세요.");
        }
    }

    /** 작성자가 탈퇴해 조회되지 않으면 어드민이 아니므로 삭제를 허용한다. */
    static void requireCanDeleteContentOf(
            GalleryAuthorityPort authorityPort, GalleryActor actor, Long authorId, Long galleryId, LocalDateTime now
    ) {
        boolean allowed = authorityPort.findActor(authorId, galleryId, now)
                .map(author -> GalleryModerationPolicy.canDeleteContentOf(actor, author))
                .orElse(true);
        if (!allowed) {
            throw new GalleryModerationApplicationException(
                    GalleryModerationErrorCode.FORBIDDEN, "관리자가 작성한 글과 댓글은 삭제할 수 없습니다.");
        }
    }

    static GalleryModerationApplicationException galleryNotFound() {
        return new GalleryModerationApplicationException(
                GalleryModerationErrorCode.GALLERY_NOT_FOUND, "존재하지 않거나 삭제된 갤러리입니다.");
    }

    static GalleryModerationApplicationException forbidden() {
        return new GalleryModerationApplicationException(
                GalleryModerationErrorCode.FORBIDDEN, "커뮤니티 어드민 또는 운영자 어드민만 할 수 있습니다.");
    }

    static void validatePage(Long cursor, int size) {
        if ((cursor != null && cursor < 1) || size < 1 || size > 100) {
            throw new GalleryModerationApplicationException(
                    GalleryModerationErrorCode.VALIDATION_ERROR, "cursor와 size 값을 확인해 주세요.");
        }
    }
}
