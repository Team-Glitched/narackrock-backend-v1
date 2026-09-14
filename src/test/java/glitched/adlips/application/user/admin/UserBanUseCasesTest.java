package glitched.adlips.application.user.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import glitched.adlips.application.user.admin.dto.request.UserBanCancelRequest;
import glitched.adlips.application.user.admin.dto.request.UserBanCreateRequest;
import glitched.adlips.application.user.admin.dto.response.UserBanCreateResponse;
import glitched.adlips.application.user.admin.port.out.UserBanRepositoryPort;
import glitched.adlips.application.user.admin.usecase.UserBanCancelUseCase;
import glitched.adlips.application.user.admin.usecase.UserBanCreateUseCase;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.admin.UserBan;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserBanUseCasesTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-09T00:00:00Z"),
            ZoneOffset.UTC
    );

    private InMemoryUsers users;
    private InMemoryBans bans;
    private UserBanCreateUseCase createUseCase;
    private UserBanCancelUseCase cancelUseCase;

    @BeforeEach
    void setUp() {
        users = new InMemoryUsers();
        bans = new InMemoryBans();
        createUseCase = new UserBanCreateUseCase(users, bans, CLOCK);
        cancelUseCase = new UserBanCancelUseCase(users, bans, CLOCK);
        users.save(User.restore(1L, "admin@example.com", UserRole.ADMIN, null));
        users.save(User.restore(2L, "user@example.com", UserRole.USER, null));
    }

    @Test
    void createsTemporaryBan() {
        UserBanCreateResponse response = createUseCase.execute(
                new UserBanCreateRequest(1L, 2L, 7, "spam")
        );

        assertEquals(2L, response.userId());
        assertEquals(7, response.banDurationDays());
        assertEquals(LocalDateTime.of(2026, 7, 16, 0, 0), response.bannedUntil());
    }

    @Test
    void createsPermanentBanWhenDurationIsMinusOne() {
        UserBanCreateResponse response = createUseCase.execute(
                new UserBanCreateRequest(1L, 2L, -1, "abuse")
        );

        assertEquals(-1, response.banDurationDays());
        assertNull(response.bannedUntil());
    }

    @Test
    void rejectsNonAdmin() {
        UserApplicationException exception = assertThrows(
                UserApplicationException.class,
                () -> createUseCase.execute(new UserBanCreateRequest(2L, 1L, 7, "spam"))
        );

        assertEquals(UserErrorCode.FORBIDDEN_ADMIN_ACCESS, exception.getErrorCode());
    }

    @Test
    void rejectsAlreadyBannedUser() {
        createUseCase.execute(new UserBanCreateRequest(1L, 2L, 7, "spam"));

        UserApplicationException exception = assertThrows(
                UserApplicationException.class,
                () -> createUseCase.execute(new UserBanCreateRequest(1L, 2L, 7, "spam again"))
        );

        assertEquals(UserErrorCode.ALREADY_BANNED_USER, exception.getErrorCode());
    }

    @Test
    void cancelsActiveBan() {
        createUseCase.execute(new UserBanCreateRequest(1L, 2L, 7, "spam"));

        cancelUseCase.execute(new UserBanCancelRequest(1L, 2L));

        assertEquals(Optional.empty(), bans.findActiveByUserId(2L, LocalDateTime.now(CLOCK)));
    }

    @Test
    void rejectsCancelWhenBanDoesNotExist() {
        UserApplicationException exception = assertThrows(
                UserApplicationException.class,
                () -> cancelUseCase.execute(new UserBanCancelRequest(1L, 2L))
        );

        assertEquals(UserErrorCode.BAN_NOT_FOUND, exception.getErrorCode());
    }

    private static final class InMemoryUsers implements UserRepositoryPort {
        private final Map<Long, User> values = new HashMap<>();

        @Override
        public Optional<User> findById(Long id) {
            return Optional.ofNullable(values.get(id));
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return values.values().stream()
                    .filter(user -> user.getEmail().equals(email))
                    .findFirst();
        }

        @Override
        public User save(User user) {
            values.put(user.getId(), user);
            return user;
        }
    }

    private static final class InMemoryBans implements UserBanRepositoryPort {
        private final Map<Long, UserBan> values = new HashMap<>();
        private long sequence = 1L;

        @Override
        public Optional<UserBan> findActiveByUserId(Long userId, LocalDateTime now) {
            return values.values().stream()
                    .filter(ban -> ban.getUser().getId().equals(userId))
                    .filter(ban -> ban.isActiveAt(now))
                    .findFirst();
        }

        @Override
        public UserBan save(UserBan userBan) {
            UserBan saved = userBan.getId() == null
                    ? UserBan.restore(
                            sequence++,
                            userBan.getUser(),
                            userBan.getAdmin(),
                            userBan.getReason(),
                            userBan.getBannedUntil(),
                            userBan.getLiftedAt()
                    )
                    : userBan;
            values.put(saved.getId(), saved);
            return saved;
        }
    }
}
