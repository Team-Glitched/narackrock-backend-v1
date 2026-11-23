package glitched.adlips.application.user.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import glitched.adlips.application.user.account.port.out.RefreshTokenGeneratorPort;
import glitched.adlips.application.user.account.port.out.UserRefreshTokenRepositoryPort;
import glitched.adlips.application.user.account.usecase.RefreshTokenManager;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.domain.user.UserRefreshToken;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RefreshTokenManagerTest {

    private static final Duration REUSE_GRACE = Duration.ofSeconds(60);

    private InMemoryRefreshTokens repository;
    private SequencedTokenGenerator generator;
    private MutableClock clock;
    private RefreshTokenManager manager;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRefreshTokens();
        generator = new SequencedTokenGenerator();
        clock = new MutableClock(Instant.parse("2026-06-30T00:00:00Z"));
        manager = new RefreshTokenManager(repository, generator, clock, 2_592_000L, REUSE_GRACE);
    }

    @Test
    void issuesRefreshTokenAndStoresOnlyHash() {
        String rawToken = manager.issue(1L);

        assertEquals("refresh-1", rawToken);
        assertTrue(repository.findByTokenHash("hash:refresh-1").isPresent());
        assertFalse(repository.findByTokenHash("refresh-1").isPresent());
    }

    @Test
    void rotatesRefreshTokenAndRevokesPreviousToken() {
        String previous = manager.issue(1L);

        RefreshTokenManager.Rotation rotation = manager.rotate(previous);

        assertEquals(1L, rotation.userId());
        assertEquals("refresh-2", rotation.refreshToken());
        assertTrue(repository.findByTokenHash("hash:refresh-1").orElseThrow().isRevoked());
        assertTrue(repository.findByTokenHash("hash:refresh-2").isPresent());
    }

    @Test
    void rejectsExpiredRefreshToken() {
        UserRefreshToken expired = UserRefreshToken.create(
                1L,
                "hash:expired",
                Instant.parse("2026-06-29T00:00:00Z")
        );
        repository.save(expired);

        assertInvalid("expired");
    }

    @Test
    void retriesLostRotationWithinGraceAndRevokesTheUnusedSuccessor() {
        String sent = manager.issue(1L);
        manager.rotate(sent);
        clock.advance(Duration.ofSeconds(10));

        RefreshTokenManager.Rotation retry = manager.rotate(sent);

        assertEquals(1L, retry.userId());
        assertEquals("refresh-3", retry.refreshToken());
        assertTrue(repository.findByTokenHash("hash:refresh-2").orElseThrow().isRevoked());
        assertFalse(repository.findByTokenHash("hash:refresh-3").orElseThrow().isRevoked());
        assertEquals(1L, manager.rotate("refresh-3").userId());
    }

    @Test
    void retriesNeverExtendTheGrace() {
        String sent = manager.issue(1L);
        manager.rotate(sent);
        clock.advance(Duration.ofSeconds(50));
        manager.rotate(sent);
        clock.advance(Duration.ofSeconds(20));

        assertInvalid(sent);
        assertFalse(repository.findByTokenHash("hash:refresh-3").orElseThrow().isRevoked());
    }

    @Test
    void rejectsRotatedTokenAfterGrace() {
        String sent = manager.issue(1L);
        manager.rotate(sent);
        clock.advance(REUSE_GRACE.plusSeconds(1));

        assertInvalid(sent);
        assertFalse(repository.findByTokenHash("hash:refresh-2").orElseThrow().isRevoked());
    }

    @Test
    void rejectsRotatedTokenOnceItsSuccessorWasUsed() {
        String sent = manager.issue(1L);
        String successor = manager.rotate(sent).refreshToken();
        manager.rotate(successor);

        assertInvalid(sent);
        assertFalse(repository.findByTokenHash("hash:refresh-3").orElseThrow().isRevoked());
    }

    @Test
    void rejectsTokenRevokedByLogoutWithinGrace() {
        String sent = manager.issue(1L);
        manager.revokeAll(1L);

        assertInvalid(sent);
    }

    @Test
    void rejectsRotatedTokenAfterLogoutRevokedItsSuccessor() {
        String sent = manager.issue(1L);
        manager.rotate(sent);
        manager.revokeAll(1L);

        assertInvalid(sent);
    }

    @Test
    void revokesAllActiveRefreshTokensForUser() {
        manager.issue(1L);
        manager.issue(1L);
        manager.issue(2L);

        manager.revokeAll(1L);

        assertTrue(repository.findByTokenHash("hash:refresh-1").orElseThrow().isRevoked());
        assertTrue(repository.findByTokenHash("hash:refresh-2").orElseThrow().isRevoked());
        assertFalse(repository.findByTokenHash("hash:refresh-3").orElseThrow().isRevoked());
    }

    private void assertInvalid(String rawToken) {
        UserApplicationException exception = assertThrows(
                UserApplicationException.class,
                () -> manager.rotate(rawToken)
        );
        assertEquals(UserErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private static final class SequencedTokenGenerator implements RefreshTokenGeneratorPort {
        private int sequence;

        @Override
        public String generate() {
            return "refresh-" + ++sequence;
        }

        @Override
        public String hash(String token) {
            return "hash:" + token;
        }
    }

    private static final class InMemoryRefreshTokens implements UserRefreshTokenRepositoryPort {
        private final Map<String, UserRefreshToken> values = new HashMap<>();
        private long nextId = 1;

        @Override
        public Optional<UserRefreshToken> findByTokenHashForUpdate(String tokenHash) {
            return Optional.ofNullable(values.get(tokenHash));
        }

        @Override
        public Optional<UserRefreshToken> findByIdForUpdate(Long id) {
            return values.values().stream().filter(token -> id.equals(token.getId())).findFirst();
        }

        @Override
        public List<UserRefreshToken> findActiveByUserId(Long userId) {
            return values.values().stream()
                    .filter(token -> token.getUserId().equals(userId) && !token.isRevoked())
                    .toList();
        }

        Optional<UserRefreshToken> findByTokenHash(String tokenHash) {
            return Optional.ofNullable(values.get(tokenHash));
        }

        @Override
        public UserRefreshToken save(UserRefreshToken refreshToken) {
            UserRefreshToken stored = refreshToken.getId() != null
                    ? refreshToken
                    : UserRefreshToken.restore(
                            nextId++,
                            refreshToken.getUserId(),
                            refreshToken.getTokenHash(),
                            refreshToken.getExpiresAt(),
                            refreshToken.getRevokedAt(),
                            refreshToken.getReplacedById()
                    );
            values.put(stored.getTokenHash(), stored);
            return stored;
        }
    }
}
