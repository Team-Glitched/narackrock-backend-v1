package glitched.adlips.application.user.account.port.out;

import glitched.adlips.domain.user.UserRefreshToken;
import java.util.Optional;
import java.util.List;

public interface UserRefreshTokenRepositoryPort {
    Optional<UserRefreshToken> findByTokenHashForUpdate(String tokenHash);

    List<UserRefreshToken> findActiveByUserId(Long userId);

    UserRefreshToken save(UserRefreshToken refreshToken);
}
