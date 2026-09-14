package glitched.adlips.adapter.out.persistence.user;

import glitched.adlips.application.user.admin.port.out.UserBanRepositoryPort;
import glitched.adlips.domain.admin.UserBan;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class UserBanPersistenceAdapter implements UserBanRepositoryPort {
    private final SpringDataUserBanRepository userBanRepository;

    public UserBanPersistenceAdapter(SpringDataUserBanRepository userBanRepository) {
        this.userBanRepository = userBanRepository;
    }

    @Override
    public Optional<UserBan> findActiveByUserId(Long userId, LocalDateTime now) {
        return userBanRepository.findActiveByUserId(userId, now);
    }

    @Override
    public UserBan save(UserBan userBan) {
        return userBanRepository.save(userBan);
    }
}
