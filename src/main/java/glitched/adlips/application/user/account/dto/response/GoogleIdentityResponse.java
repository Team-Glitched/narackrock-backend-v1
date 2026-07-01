package glitched.adlips.application.user.account.dto.response;

public record GoogleIdentityResponse(String subject, String email, boolean emailVerified) {
}
