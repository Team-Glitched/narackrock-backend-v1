package glitched.adlips.application.user.admin.dto.request;

public record UserBanCreateRequest(
        Long adminId,
        Long userId,
        Integer banDurationDays,
        String banReason
) {
}
