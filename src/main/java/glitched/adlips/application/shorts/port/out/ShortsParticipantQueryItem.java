package glitched.adlips.application.shorts.port.out;

public record ShortsParticipantQueryItem(
        Long shortId,
        Long userId,
        String nickname,
        String role
) {
}
