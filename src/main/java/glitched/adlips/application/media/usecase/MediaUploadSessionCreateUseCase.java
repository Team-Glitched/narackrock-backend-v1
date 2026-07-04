package glitched.adlips.application.media.usecase;

import glitched.adlips.adapter.out.persistence.media.MediaUploadSessionJpaRepository;
import glitched.adlips.application.media.MediaApplicationException;
import glitched.adlips.application.media.MediaErrorCode;
import glitched.adlips.application.media.dto.request.MediaUploadSessionCreateRequest;
import glitched.adlips.application.media.dto.response.MediaUploadSessionCreateResponse;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.media.MediaFileType;
import glitched.adlips.domain.media.MediaUploadSession;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediaUploadSessionCreateUseCase {
    private static final long MAX_FILE_SIZE = 100L * 1024 * 1024;
    private static final Map<MediaFileType, Set<String>> ALLOWED_MIME_TYPES = Map.of(
            MediaFileType.AUDIO, Set.of("audio/wav", "audio/x-wav", "audio/mpeg", "audio/mp4", "audio/flac"),
            MediaFileType.IMAGE, Set.of("image/jpeg", "image/png", "image/webp", "image/gif"),
            MediaFileType.VIDEO, Set.of("video/mp4", "video/webm")
    );

    private final MediaFileRepositoryPort mediaFiles;
    private final MediaUploadSessionJpaRepository sessions;
    private final Clock clock;
    private final String apiBaseUrl;

    @Autowired
    public MediaUploadSessionCreateUseCase(
            MediaFileRepositoryPort mediaFiles,
            MediaUploadSessionJpaRepository sessions,
            @Value("${app.api.public-base-url:http://localhost:8080}") String apiBaseUrl
    ) {
        this(mediaFiles, sessions, Clock.systemUTC(), apiBaseUrl);
    }

    public MediaUploadSessionCreateUseCase(
            MediaFileRepositoryPort mediaFiles,
            MediaUploadSessionJpaRepository sessions,
            Clock clock,
            String apiBaseUrl
    ) {
        this.mediaFiles = mediaFiles;
        this.sessions = sessions;
        this.clock = clock;
        this.apiBaseUrl = stripTrailingSlash(apiBaseUrl);
    }

    @Transactional
    public MediaUploadSessionCreateResponse execute(MediaUploadSessionCreateRequest request) {
        validate(request);
        String safeFileName = request.fileName().replaceAll("[^a-zA-Z0-9._-]", "_");
        String storageKey = "media/" + request.userId() + "/" + UUID.randomUUID() + "-" + safeFileName;
        MediaFile saved = mediaFiles.save(MediaFile.uploading(
                request.userId(), storageKey, request.fileName(), request.fileType(),
                request.mimeType(), request.fileSize()));
        String uploadUrl = apiBaseUrl + "/api/v1/media/" + saved.getId() + "/content";
        LocalDateTime expiresAt = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).plusMinutes(15);
        sessions.save(new MediaUploadSession(
                saved.getId(), request.userId(), uploadUrl, request.fileSize(), request.mimeType(), expiresAt));
        return new MediaUploadSessionCreateResponse(
                saved.getId(), uploadUrl, "PUT",
                expiresAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
    }

    private void validate(MediaUploadSessionCreateRequest request) {
        if (request == null || request.userId() == null || request.fileType() == null
                || request.fileName() == null || request.fileName().isBlank()
                || request.mimeType() == null || request.mimeType().isBlank()
                || request.fileSize() <= 0 || request.fileSize() > MAX_FILE_SIZE) {
            throw validationError();
        }
        Set<String> allowed = ALLOWED_MIME_TYPES.get(request.fileType());
        if (allowed == null || !allowed.contains(request.mimeType().toLowerCase())) {
            throw validationError();
        }
    }

    private MediaApplicationException validationError() {
        return new MediaApplicationException(
                MediaErrorCode.VALIDATION_ERROR, "파일 형식, 이름, 크기를 확인해 주세요.");
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
