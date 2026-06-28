package glitched.adlips.application.auth;

public record AuthResult(String accessToken, String tokenType, Long userId, String nickname, String profileImageUrl) {}
