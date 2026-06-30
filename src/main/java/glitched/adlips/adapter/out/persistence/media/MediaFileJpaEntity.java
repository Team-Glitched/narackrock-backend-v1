package glitched.adlips.adapter.out.persistence.media;

import glitched.adlips.domain.media.MediaFileStatus;
import glitched.adlips.domain.media.MediaFileType;
import glitched.adlips.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "media_files",
        indexes = {
                @Index(columnList = "owner_id"),
                @Index(columnList = "file_type, status")
        }
)
class MediaFileJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "storage_key")
    private String storageKey;

    @Column(name = "original_filename")
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private MediaFileType fileType;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "file_size")
    private long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaFileStatus status;

    protected MediaFileJpaEntity() {
    }

    MediaFileJpaEntity(
            Long id,
            Long ownerId,
            String fileUrl,
            String storageKey,
            String originalFilename,
            MediaFileType fileType,
            String mimeType,
            long fileSize,
            MediaFileStatus status
    ) {
        this.id = id;
        this.ownerId = ownerId;
        this.fileUrl = fileUrl;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.fileType = fileType;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.status = status;
    }

    Long getId() {
        return id;
    }

    Long getOwnerId() {
        return ownerId;
    }

    String getFileUrl() {
        return fileUrl;
    }

    String getStorageKey() {
        return storageKey;
    }

    String getOriginalFilename() {
        return originalFilename;
    }

    MediaFileType getFileType() {
        return fileType;
    }

    String getMimeType() {
        return mimeType;
    }

    long getFileSize() {
        return fileSize;
    }

    MediaFileStatus getStatus() {
        return status;
    }
}
