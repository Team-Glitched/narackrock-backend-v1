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
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RefreshTokenManagerTest {

    private InMemoryRefreshTokens repository;
    private SequencedTokenGenerator generator;
    private RefreshTokenManager manager;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRefreshTokens();
        generator = new SequencedTokenGenerator();
        Clock clock = Clock.fixed(Instant.parse("2026-06-30T00:00:00Z"), ZoneOffset.UTC);
        manager = new RefreshTokenManager(repository, generator, clock, 2_592_000L);
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

        UserApplicationException exception = assertThrows(
                UserApplicationException.class,
                () -> manager.rotate("expired")
        );

        assertEquals(UserErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
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

        @Override
        public Optional<UserRefreshToken> findByTokenHashForUpdate(String tokenHash) {
            return Optional.ofNullable(values.get(tokenHash));
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
            values.put(refreshToken.getTokenHash(), refreshToken);
            return refreshToken;
        }
    }
}
