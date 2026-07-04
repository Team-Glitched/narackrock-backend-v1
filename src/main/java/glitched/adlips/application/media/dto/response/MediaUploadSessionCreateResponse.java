package glitched.adlips.application.media.dto.response;

public record MediaUploadSessionCreateResponse(
        Long mediaFileId,
        String uploadUrl,
        String method,
        String expiresAt
) {
}
