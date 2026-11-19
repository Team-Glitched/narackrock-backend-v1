package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.community.port.out.GalleryManagementPort;
import glitched.adlips.application.community.port.out.GalleryModerationLogPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.community.GalleryModerationActionType;
import glitched.adlips.domain.media.MediaFileStatus;
import glitched.adlips.domain.media.MediaFileType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

public class UpdateGalleryImagesUseCase {
    private final GalleryAuthorityPort authority;
    private final GalleryManagementPort management;
    private final GalleryModerationLogPort logs;
    private final MediaFileRepositoryPort media;
    private final TransactionRunner transactions;
    private final Clock clock;

    public UpdateGalleryImagesUseCase(GalleryAuthorityPort authority, GalleryManagementPort management,
            GalleryModerationLogPort logs, MediaFileRepositoryPort media, TransactionRunner transactions, Clock clock) {
        this.authority = authority;
        this.management = management;
        this.logs = logs;
        this.media = media;
        this.transactions = transactions;
        this.clock = clock;
    }

    public void execute(Long actorId, Long galleryId, Long imageFileId, Long bannerImageFileId) {
        transactions.required(() -> {
            var gallery = management.findGalleryForUpdate(galleryId).orElseThrow(GalleryModerationGuard::galleryNotFound);
            GalleryModerationGuard.requireManager(authority, actorId, galleryId, LocalDateTime.now(clock));
            validateNewImage(actorId, imageFileId, gallery.getImageFileId());
            validateNewImage(actorId, bannerImageFileId, gallery.getBannerImageFileId());
            gallery.changeImages(imageFileId, bannerImageFileId);
            management.updateGallery(gallery);
            logs.save(galleryId, actorId, GalleryModerationActionType.UPDATE_GALLERY, null, galleryId, null);
            return null;
        });
    }

    private void validateNewImage(Long actorId, Long next, Long current) {
        if (next == null || Objects.equals(next, current)) return;
        if (next < 1) throw invalidImage();
        var file = media.findById(next).orElseThrow(this::invalidImage);
        if (!actorId.equals(file.getOwnerId())
                || file.getFileType() != MediaFileType.IMAGE
                || file.getStatus() != MediaFileStatus.READY) {
            throw invalidImage();
        }
    }

    private GalleryModerationApplicationException invalidImage() {
        return new GalleryModerationApplicationException(GalleryModerationErrorCode.VALIDATION_ERROR,
                "업로드가 완료된 본인 이미지 파일을 선택해 주세요.");
    }
}
