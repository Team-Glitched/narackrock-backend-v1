package glitched.adlips.application.media.usecase;

import glitched.adlips.adapter.out.persistence.media.MediaUploadSessionJpaRepository;
import glitched.adlips.adapter.out.storage.LocalFileStorageAdapter;
import glitched.adlips.application.media.MediaApplicationException;
import glitched.adlips.application.media.MediaErrorCode;
import glitched.adlips.application.media.dto.request.MediaUploadCompleteRequest;
import glitched.adlips.application.media.dto.response.MediaUploadCompleteResponse;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.media.MediaFileStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediaUploadCompleteUseCase {
    private final MediaFileRepositoryPort mediaFiles;
    private final MediaUploadSessionJpaRepository sessions;
    private final LocalFileStorageAdapter storage;
    private final Clock clock;

    @Autowired
    public MediaUploadCompleteUseCase(MediaFileRepositoryPort mediaFiles,
                                      MediaUploadSessionJpaRepository sessions,
                                      LocalFileStorageAdapter storage) {
        this(mediaFiles, sessions, storage, Clock.systemUTC());
    }

    public MediaUploadCompleteUseCase(MediaFileRepositoryPort mediaFiles,
                                      MediaUploadSessionJpaRepository sessions,
                                      LocalFileStorageAdapter storage,
                                      Clock clock) {
        this.mediaFiles = mediaFiles;
        this.sessions = sessions;
        this.storage = storage;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = MediaApplicationException.class)
    public MediaUploadCompleteResponse execute(MediaUploadCompleteRequest request) {
        MediaFile media = mediaFiles.findById(request.mediaFileId())
                .orElseThrow(() -> error(MediaErrorCode.MEDIA_FILE_NOT_FOUND, "존재하지 않는 미디어 파일입니다."));
        if (!media.getOwnerId().equals(request.userId())) {
            throw error(MediaErrorCode.MEDIA_FILE_ACCESS_DENIED, "해당 미디어 파일에 접근할 권한이 없습니다.");
        }
        if (media.getStatus() != MediaFileStatus.UPLOADING) {
            throw error(MediaErrorCode.MEDIA_FILE_NOT_READY, "업로드 완료 처리할 수 없는 파일 상태입니다.");
        }
        var session = sessions.findFirstByMediaFileIdOrderByCreatedAtDesc(request.mediaFileId())
                .orElseThrow(() -> error(MediaErrorCode.MEDIA_FILE_NOT_FOUND, "업로드 세션을 찾을 수 없습니다."));
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        if (session.isExpiredAt(now)) {
            throw error(MediaErrorCode.UPLOAD_SESSION_EXPIRED, "업로드 URL이 만료되었습니다.");
        }
        if (!storage.exists(media.getStorageKey())) {
            fail(media);
            throw error(MediaErrorCode.UPLOADED_OBJECT_NOT_FOUND,
                    "업로드된 파일을 스토리지에서 찾을 수 없습니다. 파일 업로드를 완료한 뒤 다시 시도해 주세요.");
        }
        if (storage.size(media.getStorageKey()) != session.getRequestedFileSize()) {
            fail(media);
            throw error(MediaErrorCode.MEDIA_FILE_SIZE_MISMATCH,
                    "업로드된 파일 크기가 요청한 파일 크기와 일치하지 않습니다.");
        }
        if (!session.getRequestedMimeType().equalsIgnoreCase(storage.contentType(media.getStorageKey()))) {
            fail(media);
            throw error(MediaErrorCode.MEDIA_MIME_TYPE_MISMATCH,
                    "업로드된 파일의 형식이 요청한 파일 형식과 일치하지 않습니다.");
        }

        MediaFile processing = media.processing();
        mediaFiles.save(processing);
        mediaFiles.save(processing.ready(storage.publicUrl(media.getStorageKey())));
        session.complete(now);
        sessions.save(session);
        return new MediaUploadCompleteResponse(media.getId(), MediaFileStatus.PROCESSING.name());
    }

    private void fail(MediaFile media) {
        mediaFiles.save(media.failed());
    }

    private MediaApplicationException error(MediaErrorCode code, String message) {
        return new MediaApplicationException(code, message);
    }
}
