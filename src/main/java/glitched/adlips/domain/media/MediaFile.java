package glitched.adlips.domain.media;

import java.util.Objects;

public final class MediaFile {
    private final Long id;
    private final Long ownerId;
    private final String fileUrl;
    private final String storageKey;
    private final String originalFilename;
    private final MediaFileType fileType;
    private final String mimeType;
    private final long fileSize;
    private final MediaFileStatus status;

    private MediaFile(
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
        this.ownerId = Objects.requireNonNull(ownerId);
        this.fileUrl = requireText(fileUrl);
        this.storageKey = requireText(storageKey);
        this.originalFilename = originalFilename;
        this.fileType = Objects.requireNonNull(fileType);
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.status = Objects.requireNonNull(status);
    }

    public static MediaFile readyImage(
            Long ownerId,
            String fileUrl,
            String storageKey,
            String originalFilename,
            String mimeType,
            long fileSize
    ) {
        return new MediaFile(
                null, ownerId, fileUrl, storageKey, originalFilename,
                MediaFileType.IMAGE, mimeType, fileSize, MediaFileStatus.READY
        );
    }

    public static MediaFile restore(
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
        return new MediaFile(
                id, ownerId, fileUrl, storageKey, originalFilename,
                fileType, mimeType, fileSize, status
        );
    }

    public MediaFile withId(Long id) {
        return new MediaFile(
                Objects.requireNonNull(id), ownerId, fileUrl, storageKey, originalFilename,
                fileType, mimeType, fileSize, status
        );
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public MediaFileType getFileType() {
        return fileType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public MediaFileStatus getStatus() {
        return status;
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("파일 경로는 필수입니다.");
        }
        return value;
    }
}
