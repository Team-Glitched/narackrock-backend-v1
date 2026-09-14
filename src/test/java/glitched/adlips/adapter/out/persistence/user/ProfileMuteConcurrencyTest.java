package glitched.adlips.adapter.out.persistence.user;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.user.profile.usecase.ProfileMuteToggleUseCase;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.auth.token-secret=test-secret-key-at-least-32-characters-long")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProfileMuteConcurrencyTest {

    private static final long USER_ID = 992L;

    @Autowired ProfileMuteToggleUseCase profileMuteToggleUseCase;
    @Autowired JdbcTemplate jdbcTemplate;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(2);
        cleanUpData();
        jdbcTemplate.update("""
                INSERT INTO users (id, email, role, created_at)
                VALUES (?, 'mute-concurrency@test.com', 'USER', CURRENT_TIMESTAMP)
                """, USER_ID);
        jdbcTemplate.update("""
                INSERT INTO profiles (user_id, nickname, is_private, follower_count, following_count)
                VALUES (?, 'mute_concurrency', false, 0, 0)
                """, USER_ID);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executorService.shutdownNow();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
        cleanUpData();
    }

    @Test
    void 음소거_토글이_동시에_두_번_호출되어도_최종_상태가_일치한다() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<Boolean> first = executorService.submit(() -> toggleWhenStarted(ready, start));
        Future<Boolean> second = executorService.submit(() -> toggleWhenStarted(ready, start));

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        assertThat(isMuted()).isFalse();
    }

    private boolean toggleWhenStarted(CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("동시성 테스트 시작을 기다리다 시간이 초과되었습니다.");
        }
        return profileMuteToggleUseCase.execute(USER_ID);
    }

    private boolean isMuted() {
        Boolean result = jdbcTemplate.queryForObject(
                "SELECT is_muted FROM profiles WHERE user_id = ?",
                Boolean.class,
                USER_ID
        );
        return Boolean.TRUE.equals(result);
    }

    private void cleanUpData() {
        jdbcTemplate.update("DELETE FROM profiles WHERE user_id = ?", USER_ID);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", USER_ID);
    }
}
