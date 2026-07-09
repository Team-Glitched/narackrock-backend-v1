package glitched.adlips.application.user.admin.dto.response;

import java.time.LocalDateTime;

public record UserBanCreateResponse(
        Long userId,
        int banDurationDays,
        LocalDateTime bannedUntil
) {
}
