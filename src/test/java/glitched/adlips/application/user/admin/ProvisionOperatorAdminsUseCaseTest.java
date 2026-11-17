package glitched.adlips.application.user.admin;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.admin.usecase.ProvisionOperatorAdminsUseCase;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserRole;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProvisionOperatorAdminsUseCaseTest {

    private InMemoryUsers users;

    @BeforeEach
    void setUp() {
        users = new InMemoryUsers();
        users.save(User.restore(1L, "op@test.local", UserRole.USER, null));
        users.save(User.restore(2L, "member@test.local", UserRole.USER, null));
        users.save(User.restore(3L, "gone@test.local", UserRole.USER, LocalDateTime.of(2026, 1, 1, 0, 0)));
    }

    private ProvisionOperatorAdminsUseCase useCase(String emails) {
        return new ProvisionOperatorAdminsUseCase(users, TransactionRunner.direct(), emails);
    }

    @Test
    void 설정된_이메일의_사용자를_운영자_어드민으로_올리고_다시_실행하면_변경이_없다() {
        ProvisionOperatorAdminsUseCase useCase = useCase(" OP@test.local , ,x@test.local");

        assertThat(useCase.execute()).isEqualTo(1);
        assertThat(users.findById(1L).orElseThrow().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(users.findById(2L).orElseThrow().getRole()).isEqualTo(UserRole.USER);
        assertThat(useCase.execute()).isZero();
    }

    @Test
    void 탈퇴한_사용자는_올리지_않는다() {
        assertThat(useCase("gone@test.local").execute()).isZero();
        assertThat(users.findById(3L).orElseThrow().getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void 설정이_비었으면_아무것도_하지_않는다() {
        assertThat(useCase("").execute()).isZero();
        assertThat(useCase(" , ").execute()).isZero();
        assertThat(users.findById(1L).orElseThrow().getRole()).isEqualTo(UserRole.USER);
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
}
