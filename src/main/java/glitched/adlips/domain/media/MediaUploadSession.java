package glitched.adlips.domain.media;

import glitched.adlips.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "media_upload_sessions", indexes = {
        @Index(name = "idx_media_upload_session_file", columnList = "media_file_id"),
        @Index(name = "idx_media_upload_session_user", columnList = "user_id"),
        @Index(name = "idx_media_upload_session_expires", columnList = "expires_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MediaUploadSession extends BaseCreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "media_file_id", nullable = false)
    private Long mediaFileId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "upload_url", nullable = false)
    private String uploadUrl;

    @Column(name = "method", nullable = false)
    private String method = "PUT";

    @Column(name = "requested_file_size")
    private long requestedFileSize;

    @Column(name = "requested_mime_type")
    private String requestedMimeType;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public MediaUploadSession(Long mediaFileId, Long userId, String uploadUrl,
                              long requestedFileSize, String requestedMimeType,
                              LocalDateTime expiresAt) {
        this.mediaFileId = Objects.requireNonNull(mediaFileId);
        this.userId = Objects.requireNonNull(userId);
        this.uploadUrl = Objects.requireNonNull(uploadUrl);
        this.requestedFileSize = requestedFileSize;
        this.requestedMimeType = Objects.requireNonNull(requestedMimeType);
        this.expiresAt = Objects.requireNonNull(expiresAt);
    }

    public boolean isExpiredAt(LocalDateTime time) {
        return !expiresAt.isAfter(time);
    }
}
