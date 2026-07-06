package glitched.adlips.adapter.out.persistence.user;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.domain.user.Profile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ProfileMutePersistenceTest {

    @Autowired SpringDataProfileRepository profileRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void 프로필_음소거_토글_조회는_비관적_쓰기_잠금을_사용한다() throws Exception {
        Method method = SpringDataProfileRepository.class
                .getDeclaredMethod("findByUserIdForUpdate", Long.class);

        assertThat(method.getAnnotation(Lock.class).value())
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void 음소거_컬럼을_생략하면_false가_기본값으로_저장된다() {
        jdbcTemplate.update("""
                INSERT INTO users (id, email, role, created_at)
                VALUES (991, 'mute-default@test.com', 'USER', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO profiles (user_id, nickname, is_private, follower_count, following_count)
                VALUES (991, 'mute_default', false, 0, 0)
                """);
        entityManager.clear();

        Profile profile = profileRepository.findByUserIdForUpdate(991L).orElseThrow();

        assertThat(profile.isMuted()).isFalse();
    }
}
