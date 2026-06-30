package glitched.adlips.application.user.account;

public record AuthResult(String accessToken, String tokenType, AuthenticatedUser user) {
    public record AuthenticatedUser(Long userId, String nickname, String profileImageUrl) {
    }
}
