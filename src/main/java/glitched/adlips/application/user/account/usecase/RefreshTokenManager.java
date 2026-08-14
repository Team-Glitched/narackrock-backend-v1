package glitched.adlips.application.user.account.usecase;

import glitched.adlips.application.user.account.port.out.RefreshTokenGeneratorPort;
import glitched.adlips.application.user.account.port.out.UserRefreshTokenRepositoryPort;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.user.UserRefreshToken;
import java.time.Clock;
import java.time.Instant;
public class RefreshTokenManager {
    private final UserRefreshTokenRepositoryPort repository;
    private final RefreshTokenGeneratorPort generator;
    private final Clock clock;
    private final long validitySeconds;
    private final TransactionRunner transactionRunner;

    public RefreshTokenManager(
            UserRefreshTokenRepositoryPort repository,
            RefreshTokenGeneratorPort generator,
            Clock clock,
            long validitySeconds
    ) {
        this(repository, generator, clock, validitySeconds, TransactionRunner.direct());
    }

    public RefreshTokenManager(
            UserRefreshTokenRepositoryPort repository,
            RefreshTokenGeneratorPort generator,
            Clock clock,
            long validitySeconds,
            TransactionRunner transactionRunner
    ) {
        this.repository = repository;
        this.generator = generator;
        this.clock = clock;
        this.validitySeconds = validitySeconds;
        this.transactionRunner = transactionRunner;
    }

    public String issue(Long userId) {
        return transactionRunner.required(() -> issueInternal(userId));
    }

    private String issueInternal(Long userId) {
        String rawToken = generator.generate();
        repository.save(UserRefreshToken.create(
                userId,
                generator.hash(rawToken),
                Instant.now(clock).plusSeconds(validitySeconds)
        ));
        return rawToken;
    }

    public Rotation rotate(String rawToken) {
        return transactionRunner.required(() -> rotateInternal(rawToken));
    }

    private Rotation rotateInternal(String rawToken) {
        Instant now = Instant.now(clock);
        UserRefreshToken current = repository.findByTokenHashForUpdate(generator.hash(rawToken))
                .filter(token -> token.isUsable(now))
                .orElseThrow(this::invalidRefreshToken);
        repository.save(current.revoke(now));
        return new Rotation(current.getUserId(), issue(current.getUserId()));
    }

    public void revokeAll(Long userId) {
        transactionRunner.required(() -> {
            revokeAllInternal(userId);
            return null;
        });
    }

    private void revokeAllInternal(Long userId) {
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
