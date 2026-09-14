package glitched.adlips.application.media.usecase;

import glitched.adlips.application.media.MediaApplicationException;
import glitched.adlips.application.media.MediaErrorCode;
import glitched.adlips.application.media.port.out.MediaContentStoragePort;
import glitched.adlips.application.media.port.out.MediaUploadSessionRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFileStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
public class LocalMediaContentUploadUseCase {
    private final MediaFileRepositoryPort mediaFiles;
    private final MediaUploadSessionRepositoryPort sessions;
    private final MediaContentStoragePort storage;
    private final Clock clock = Clock.systemUTC();

    public LocalMediaContentUploadUseCase(MediaFileRepositoryPort mediaFiles,
                                          MediaUploadSessionRepositoryPort sessions,
                                          MediaContentStoragePort storage) {
        this.mediaFiles = mediaFiles;
        this.sessions = sessions;
        this.storage = storage;
    }

    public void execute(Long mediaFileId, Long userId, String contentType, byte[] content) {
        var media = mediaFiles.findById(mediaFileId)
                .orElseThrow(() -> new MediaApplicationException(
                        MediaErrorCode.MEDIA_FILE_NOT_FOUND, "존재하지 않는 미디어 파일입니다."));
        if (!media.getOwnerId().equals(userId)) {
            throw new MediaApplicationException(
                    MediaErrorCode.MEDIA_FILE_ACCESS_DENIED, "해당 미디어 파일에 접근할 권한이 없습니다.");
        }
        if (media.getStatus() != MediaFileStatus.UPLOADING) {
            throw new MediaApplicationException(
                    MediaErrorCode.MEDIA_FILE_NOT_READY, "업로드할 수 없는 파일 상태입니다.");
        }
        var session = sessions.findFirstByMediaFileIdOrderByCreatedAtDesc(mediaFileId)
                .orElseThrow(() -> new MediaApplicationException(
                        MediaErrorCode.MEDIA_FILE_NOT_FOUND, "업로드 세션을 찾을 수 없습니다."));
        if (session.isExpiredAt(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC))) {
            throw new MediaApplicationException(
                    MediaErrorCode.UPLOAD_SESSION_EXPIRED, "업로드 URL이 만료되었습니다.");
        }
        if (content == null || content.length == 0 || !session.getRequestedMimeType().equalsIgnoreCase(contentType)) {
            throw new MediaApplicationException(
                    MediaErrorCode.MEDIA_MIME_TYPE_MISMATCH, "업로드 파일 형식이 요청한 형식과 일치하지 않습니다.");
        }
        storage.put(media.getStorageKey(), content, contentType);
    }
}
