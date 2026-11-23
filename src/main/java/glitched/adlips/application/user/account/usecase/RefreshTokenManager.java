package glitched.adlips.application.user.account.usecase;

import glitched.adlips.application.user.account.port.out.RefreshTokenGeneratorPort;
import glitched.adlips.application.user.account.port.out.UserRefreshTokenRepositoryPort;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.user.UserRefreshToken;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
public class RefreshTokenManager {
    private final UserRefreshTokenRepositoryPort repository;
    private final RefreshTokenGeneratorPort generator;
    private final Clock clock;
    private final long validitySeconds;
    private final Duration reuseGrace;
    private final TransactionRunner transactionRunner;

    public RefreshTokenManager(
            UserRefreshTokenRepositoryPort repository,
            RefreshTokenGeneratorPort generator,
            Clock clock,
            long validitySeconds,
            Duration reuseGrace
    ) {
        this(repository, generator, clock, validitySeconds, reuseGrace, TransactionRunner.direct());
    }

    public RefreshTokenManager(
            UserRefreshTokenRepositoryPort repository,
            RefreshTokenGeneratorPort generator,
            Clock clock,
            long validitySeconds,
            Duration reuseGrace,
            TransactionRunner transactionRunner
    ) {
        this.repository = repository;
        this.generator = generator;
        this.clock = clock;
        this.validitySeconds = validitySeconds;
        this.reuseGrace = reuseGrace;
        this.transactionRunner = transactionRunner;
    }

    public String issue(Long userId) {
        return transactionRunner.required(() -> issueToken(userId, Instant.now(clock)).rawToken());
    }

    private Issued issueToken(Long userId, Instant now) {
        String rawToken = generator.generate();
        UserRefreshToken saved = repository.save(UserRefreshToken.create(
                userId,
                generator.hash(rawToken),
                now.plusSeconds(validitySeconds)
        ));
        return new Issued(saved.getId(), rawToken);
    }

    public Rotation rotate(String rawToken) {
        return transactionRunner.required(() -> rotateInternal(rawToken));
    }

    private Rotation rotateInternal(String rawToken) {
        Instant now = Instant.now(clock);
        UserRefreshToken presented = repository.findByTokenHashForUpdate(generator.hash(rawToken))
                .orElseThrow(this::invalidRefreshToken);
        if (!presented.isUsable(now)) {
            retireLostSuccessor(presented, now);
        }
        Issued successor = issueToken(presented.getUserId(), now);
        repository.save(presented.replaceWith(successor.id(), now));
        return new Rotation(presented.getUserId(), successor.rawToken());
    }

    /**
     * A client that lost the rotation response (app killed mid-request, dropped connection) still holds the token it
     * sent. Within the reuse grace it may rotate that token again while the token it was rotated into is still unused;
     * that unused successor is revoked so only the newest token stays valid. A used successor, a logout and an
     * expired grace all reject the token as before.
     */
    private void retireLostSuccessor(UserRefreshToken presented, Instant now) {
        if (!presented.isRotatedWithin(reuseGrace, now)) {
            throw invalidRefreshToken();
        }
        UserRefreshToken successor = repository.findByIdForUpdate(presented.getReplacedById())
                .filter(token -> token.isUsable(now))
                .orElseThrow(this::invalidRefreshToken);
        repository.save(successor.revoke(now));
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

    private record Issued(Long id, String rawToken) {
    }
}
