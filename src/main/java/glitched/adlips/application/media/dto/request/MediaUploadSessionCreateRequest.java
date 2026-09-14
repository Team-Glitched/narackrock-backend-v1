package glitched.adlips.application.media.dto.request;

import glitched.adlips.domain.media.MediaFileType;

public record MediaUploadSessionCreateRequest(
        Long userId,
        MediaFileType fileType,
        String fileName,
        String mimeType,
        long fileSize
) {
}
