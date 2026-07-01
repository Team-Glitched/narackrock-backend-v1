package glitched.adlips.application.user.relation.dto.response;

public record UserRelationResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        String primaryInstrument
) {
}
