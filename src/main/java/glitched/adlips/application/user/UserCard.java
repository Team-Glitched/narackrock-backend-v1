package glitched.adlips.application.user;

public record UserCard(
        Long userId,
        String nickname,
        String profileImageUrl,
        String primaryInstrument
) {
}
