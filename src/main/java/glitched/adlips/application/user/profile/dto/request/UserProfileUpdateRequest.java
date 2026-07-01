package glitched.adlips.application.user.profile.dto.request;

public record UserProfileUpdateRequest(
        Long userId,
        String nickname,
        String primaryInstrument,
        String explanation
) {
}
