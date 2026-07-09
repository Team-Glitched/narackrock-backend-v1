package glitched.adlips.adapter.out.persistence.admin;

import glitched.adlips.application.shorts.port.out.UserBanQueryPort;
import java.time.LocalDateTime;
import org.springframework.stereotype.Repository;

@Repository
public class UserBanQueryPersistenceAdapter implements UserBanQueryPort {

    private final UserBanJpaRepository userBanRepository;

    public UserBanQueryPersistenceAdapter(UserBanJpaRepository userBanRepository) {
        this.userBanRepository = userBanRepository;
    }

    @Override
    public boolean isBanned(Long userId, LocalDateTime now) {
        return userBanRepository.existsActiveBan(userId, now);
    }
}
