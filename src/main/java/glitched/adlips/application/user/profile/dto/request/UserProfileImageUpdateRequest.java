package glitched.adlips.application.user.profile.dto.request;

public record UserProfileImageUpdateRequest(
        Long userId,
        String originalFilename,
        String mimeType,
        byte[] content
) {
    public UserProfileImageUpdateRequest {
        content = content == null ? null : content.clone();
    }

    @Override
    public byte[] content() {
        return content == null ? null : content.clone();
    }
}
