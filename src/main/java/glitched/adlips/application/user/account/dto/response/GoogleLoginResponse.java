package glitched.adlips.application.user.account.dto.response;

public record GoogleLoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        UserResponse user
) {
    public record UserResponse(Long userId, String nickname, String profileImageUrl) {
    }
}
