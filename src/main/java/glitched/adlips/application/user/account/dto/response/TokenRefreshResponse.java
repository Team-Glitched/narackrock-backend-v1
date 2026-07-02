package glitched.adlips.application.user.account.dto.response;

public record TokenRefreshResponse(String accessToken, String refreshToken, String tokenType) {
}
