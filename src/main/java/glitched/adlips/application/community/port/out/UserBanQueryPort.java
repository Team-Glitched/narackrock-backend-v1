package glitched.adlips.application.community.port.out;

import java.time.LocalDateTime;

public interface UserBanQueryPort {
    boolean isBanned(Long userId, LocalDateTime now);
}
