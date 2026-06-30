package glitched.adlips.application.user;

public record AuthResult(String accessToken, String tokenType, AuthenticatedUser user) {
    public record AuthenticatedUser(Long userId, String nickname, String profileImageUrl) {
    }
}
