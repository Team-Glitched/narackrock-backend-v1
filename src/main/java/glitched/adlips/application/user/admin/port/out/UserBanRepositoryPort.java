package glitched.adlips.application.user.admin.port.out;

import glitched.adlips.domain.admin.UserBan;
import java.time.LocalDateTime;
import java.util.Optional;

public interface UserBanRepositoryPort {
    Optional<UserBan> findActiveByUserId(Long userId, LocalDateTime now);

    UserBan save(UserBan userBan);
}
