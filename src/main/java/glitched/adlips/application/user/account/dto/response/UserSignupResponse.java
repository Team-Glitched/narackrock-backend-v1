package glitched.adlips.application.user.account.dto.response;

public record UserSignupResponse(String accessToken, String tokenType, UserResponse user) {
    public record UserResponse(Long userId, String nickname, String profileImageUrl) {
    }
}
