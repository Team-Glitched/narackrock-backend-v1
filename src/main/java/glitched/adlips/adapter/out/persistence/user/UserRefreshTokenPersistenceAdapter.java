package glitched.adlips.adapter.out.persistence.user;

import glitched.adlips.application.user.account.port.out.UserRefreshTokenRepositoryPort;
import glitched.adlips.domain.user.UserRefreshToken;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class UserRefreshTokenPersistenceAdapter implements UserRefreshTokenRepositoryPort {
    private final SpringDataUserRefreshTokenRepository repository;

    public UserRefreshTokenPersistenceAdapter(SpringDataUserRefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<UserRefreshToken> findByTokenHashForUpdate(String tokenHash) {
        return repository.findByTokenHashForUpdate(tokenHash);
    }

    @Override
    public List<UserRefreshToken> findActiveByUserId(Long userId) {
        return repository.findAllByUserIdAndRevokedAtIsNull(userId);
    }

    @Override
    public UserRefreshToken save(UserRefreshToken refreshToken) {
        return repository.save(refreshToken);
    }
}
