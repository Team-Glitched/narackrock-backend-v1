package glitched.adlips.application.shorts.port.out;

import java.time.LocalDateTime;

public interface UserBanQueryPort {
    boolean isBanned(Long userId, LocalDateTime now);
}
