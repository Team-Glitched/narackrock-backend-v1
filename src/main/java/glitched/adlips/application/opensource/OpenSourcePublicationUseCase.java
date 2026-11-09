package glitched.adlips.application.opensource;

import glitched.adlips.application.notification.NotificationEvents;
import static glitched.adlips.domain.notification.NotificationType.*;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFileStatus;
import glitched.adlips.domain.media.MediaFileType;
import glitched.adlips.domain.opensource.OpenSourcePublication;
import java.time.Clock;
import static glitched.adlips.application.opensource.OpenSourceException.Code.*;

public class OpenSourcePublicationUseCase {
    private final NotificationEvents notifications;
    public record Patch(boolean hasTitle, String title, boolean hasDescription, String description,
                        boolean hasThumbnail, Long thumbnailMediaFileId) {}
    private final OpenSourcePublicationPort publications;
    private final MediaFileRepositoryPort media;
    private final TransactionRunner transactions;
    private final Clock clock;

    public OpenSourcePublicationUseCase(OpenSourcePublicationPort publications, MediaFileRepositoryPort media,
                                        TransactionRunner transactions, Clock clock) {
        this(publications, media, transactions, clock, NotificationEvents.none());
    }

    public OpenSourcePublicationUseCase(OpenSourcePublicationPort publications, MediaFileRepositoryPort media,
                                        TransactionRunner transactions, Clock clock, NotificationEvents notifications) {
        this.publications = publications;
        this.media = media;
        this.transactions = transactions;
        this.clock = clock;
    
        this.notifications = notifications;
    }

    public OpenSourcePublication create(Long userId, String title, String description, Long mediaFileId, Long thumbnailMediaFileId) {
        validId(userId);
        String normalizedTitle = title(title);
        description(description);
        return transactions.required(() -> {
            validateMedia(userId, mediaFileId, MediaFileType.AUDIO);
            if (thumbnailMediaFileId != null) validateMedia(userId, thumbnailMediaFileId, MediaFileType.IMAGE);
            var now = clock.instant();
            return publications.save(new OpenSourcePublication(null, userId, normalizedTitle, description,
                    mediaFileId, thumbnailMediaFileId, false, now, now, null));
        });
    }

    public OpenSourcePublication get(Long userId, Long id) {
        return transactions.readOnly(() -> owned(userId, id, false, false));
    }

    public OpenSourcePublication update(Long userId, Long id, Patch patch) {
        if (patch == null || !(patch.hasTitle() || patch.hasDescription() || patch.hasThumbnail())) throw invalid();
        return transactions.required(() -> {
            var current = owned(userId, id, true, false);
            String title = patch.hasTitle() ? title(patch.title()) : current.title();
            String description = patch.hasDescription() ? patch.description() : current.description();
            description(description);
            Long thumbnail = patch.hasThumbnail() ? patch.thumbnailMediaFileId() : current.thumbnailMediaFileId();
            if (patch.hasThumbnail() && thumbnail != null) validateMedia(userId, thumbnail, MediaFileType.IMAGE);
            return publications.save(current.edit(title, description, thumbnail, clock.instant()));
        });
    }

    public OpenSourcePublication visibility(Long userId, Long id, boolean visible) {
        return transactions.required(() -> {
            var current = owned(userId, id, true, false);
            if (visible) {
                title(current.title());
                description(current.description());
                validateMedia(userId, current.mediaFileId(), MediaFileType.AUDIO);
                if (current.thumbnailMediaFileId() != null) validateMedia(userId, current.thumbnailMediaFileId(), MediaFileType.IMAGE);
            }
            var saved=publications.save(current.visibility(visible, clock.instant()));
            if(visible&&!current.isPublic())notifications.record(FOLLOWING_SOURCE_PUBLISHED,userId,id);
            return saved;
        });
    }

    public OpenSourcePublication delete(Long userId, Long id) {
        return transactions.required(() -> publications.save(owned(userId, id, true, true).delete(clock.instant())));
    }

    private OpenSourcePublication owned(Long userId, Long id, boolean lock, boolean includeDeleted) {
        validId(userId);
        validId(id);
        return publications.findOwned(id, userId, lock)
                .filter(p -> includeDeleted || p.deletedAt() == null)
                .orElseThrow(() -> new OpenSourceException(OPEN_SOURCE_NOT_FOUND, "존재하지 않거나 접근할 수 없는 오픈소스입니다."));
    }

    private void validateMedia(Long userId, Long mediaFileId, MediaFileType type) {
        validId(mediaFileId);
        var file = media.findById(mediaFileId).filter(m -> m.getOwnerId().equals(userId))
                .orElseThrow(() -> new OpenSourceException(MEDIA_FILE_NOT_FOUND, "존재하지 않거나 접근할 수 없는 미디어입니다."));
        if (file.getStatus() != MediaFileStatus.READY) throw new OpenSourceException(MEDIA_FILE_NOT_READY, "업로드가 완료되지 않은 미디어입니다.");
        if (file.getFileType() != type) throw invalid();
    }
    private String title(String title) {
        if (title == null || title.strip().isEmpty() || title.strip().codePointCount(0, title.strip().length()) > 100) throw invalid();
        return title.strip();
    }
    private void description(String description) {
        if (description != null && description.codePointCount(0, description.length()) > 2000) throw invalid();
    }
    private void validId(Long id) { if (id == null || id <= 0) throw invalid(); }
    private OpenSourceException invalid() { return new OpenSourceException(INVALID_REQUEST, "오픈소스 입력값이 올바르지 않습니다."); }
}
