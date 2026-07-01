package glitched.adlips.application.user.account.usecase;

import glitched.adlips.application.user.account.port.out.RefreshTokenGeneratorPort;
import glitched.adlips.application.user.account.port.out.UserRefreshTokenRepositoryPort;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.domain.user.UserRefreshToken;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenManager {
    private final UserRefreshTokenRepositoryPort repository;
    private final RefreshTokenGeneratorPort generator;
    private final Clock clock;
    private final long validitySeconds;

    public RefreshTokenManager(
            UserRefreshTokenRepositoryPort repository,
            RefreshTokenGeneratorPort generator,
            Clock clock,
            @Value("${app.auth.refresh-token-validity-seconds}") long validitySeconds
    ) {
        this.repository = repository;
        this.generator = generator;
        this.clock = clock;
        this.validitySeconds = validitySeconds;
    }

    @Transactional
    public String issue(Long userId) {
        String rawToken = generator.generate();
        repository.save(UserRefreshToken.create(
                userId,
                generator.hash(rawToken),
                Instant.now(clock).plusSeconds(validitySeconds)
        ));
        return rawToken;
    }

    @Transactional
    public Rotation rotate(String rawToken) {
        Instant now = Instant.now(clock);
        UserRefreshToken current = repository.findByTokenHashForUpdate(generator.hash(rawToken))
                .filter(token -> token.isUsable(now))
                .orElseThrow(this::invalidRefreshToken);
        repository.save(current.revoke(now));
        return new Rotation(current.getUserId(), issue(current.getUserId()));
    }

    @Transactional
    public void revokeAll(Long userId) {
        Instant now = Instant.now(clock);
        repository.findActiveByUserId(userId).forEach(token -> repository.save(token.revoke(now)));
    }

    private UserApplicationException invalidRefreshToken() {
        return new UserApplicationException(
                UserErrorCode.INVALID_REFRESH_TOKEN,
                "유효하지 않거나 만료된 refresh token입니다."
        );
    }

    public record Rotation(Long userId, String refreshToken) {
    }
}
