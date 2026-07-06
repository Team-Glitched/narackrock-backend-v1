package glitched.adlips.application.shorts.port.out;

public record ShortsComposerQueryItem(
        Long userId,
        String nickname,
        String profileImageUrl,
        String role,
        String description
) {
}
