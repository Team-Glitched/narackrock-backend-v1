package glitched.adlips.adapter.out.persistence.user;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.domain.admin.UserBan;
import glitched.adlips.domain.user.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class UserBanPersistenceAdapterTest {
    @Autowired
    SpringDataUserRepository userRepository;

    @Autowired
    SpringDataUserBanRepository userBanRepository;

    @Test
    void 활성_차단만_조회한다() {
        User admin = userRepository.save(User.create("admin@test.com"));
        User user = userRepository.save(User.create("user@test.com"));
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 0, 0);

        UserBan active = userBanRepository.save(UserBan.create(
                user,
                admin,
                "spam",
                now.plusDays(7)
        ));
        userBanRepository.save(UserBan.restore(
                null,
                user,
                admin,
                "old",
                now.minusDays(1),
                null
        ));
        userBanRepository.save(UserBan.restore(
                null,
                user,
                admin,
                "lifted",
                now.plusDays(1),
                now
        ));

        assertThat(userBanRepository.findActiveByUserId(user.getId(), now))
                .contains(active);
    }

    @Test
    void 영구_차단은_활성_차단으로_조회한다() {
        User admin = userRepository.save(User.create("admin2@test.com"));
        User user = userRepository.save(User.create("user2@test.com"));
        UserBan permanent = userBanRepository.save(UserBan.create(user, admin, "abuse", null));

        assertThat(userBanRepository.findActiveByUserId(
                user.getId(),
                LocalDateTime.of(2026, 7, 9, 0, 0)
        )).contains(permanent);
    }
}
