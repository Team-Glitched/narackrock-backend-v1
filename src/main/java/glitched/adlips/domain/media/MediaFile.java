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
        this.fileUrl = status == MediaFileStatus.READY ? requireText(fileUrl) : fileUrl;
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

    public static MediaFile uploading(
            Long ownerId,
            String storageKey,
            String originalFilename,
            MediaFileType fileType,
            String mimeType,
            long fileSize
    ) {
        if (fileSize <= 0) {
            throw new IllegalArgumentException("파일 크기는 0보다 커야 합니다.");
        }
        return new MediaFile(
                null, ownerId, null, storageKey, originalFilename,
                fileType, mimeType, fileSize, MediaFileStatus.UPLOADING
        );
    }

    public static MediaFile readyGenerated(
            Long ownerId,
            String fileUrl,
            String storageKey,
            String originalFilename,
            MediaFileType fileType,
            String mimeType,
            long fileSize
    ) {
        if (fileSize <= 0) {
            throw new IllegalArgumentException("파일 크기는 0보다 커야 합니다.");
        }
        return new MediaFile(
                null, ownerId, fileUrl, storageKey, originalFilename,
                fileType, mimeType, fileSize, MediaFileStatus.READY
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

    public MediaFile processing() {
        if (status != MediaFileStatus.UPLOADING) {
            throw new IllegalStateException("업로드 중인 파일만 처리할 수 있습니다.");
        }
        return withStatus(MediaFileStatus.PROCESSING, fileUrl);
    }

    public MediaFile ready(String fileUrl) {
        if (status != MediaFileStatus.PROCESSING) {
            throw new IllegalStateException("처리 중인 파일만 완료할 수 있습니다.");
        }
        return withStatus(MediaFileStatus.READY, requireText(fileUrl));
    }

    public MediaFile failed() {
        return withStatus(MediaFileStatus.FAILED, fileUrl);
    }

    private MediaFile withStatus(MediaFileStatus status, String fileUrl) {
        return new MediaFile(id, ownerId, fileUrl, storageKey, originalFilename,
                fileType, mimeType, fileSize, status);
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
