package glitched.adlips.application.user.relation;

public record UserCard(
        Long userId,
        String nickname,
        String profileImageUrl,
        String primaryInstrument
) {
}
