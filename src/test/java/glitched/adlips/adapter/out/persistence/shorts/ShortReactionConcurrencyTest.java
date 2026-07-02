package glitched.adlips.adapter.out.persistence.shorts;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.shorts.ShortLikeResult;
import glitched.adlips.application.shorts.ToggleShortLikeUseCase;
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
class ShortReactionConcurrencyTest {

    private static final long USER_ID = 902L;
    private static final long SHORT_ID = 201L;

    @Autowired ToggleShortLikeUseCase toggleShortLikeUseCase;
    @Autowired JdbcTemplate jdbcTemplate;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(2);
        jdbcTemplate.update("DELETE FROM reactions WHERE target_type = 'SHORT' AND target_id = ?", SHORT_ID);
        jdbcTemplate.update("DELETE FROM shorts WHERE id = ?", SHORT_ID);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", USER_ID);
        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (?, 'concurrency@test.com', 'USER', CURRENT_TIMESTAMP)",
                USER_ID
        );
        jdbcTemplate.update("""
                INSERT INTO shorts (
                    id, user_id, title, media_file_id, status,
                    view_count, like_count, dislike_count, comment_count, contribution_count,
                    created_at
                ) VALUES (?, ?, 'concurrency', 1, 'COMPLETED', 0, 0, 0, 0, 0, CURRENT_TIMESTAMP)
                """, SHORT_ID, USER_ID);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executorService.shutdownNow();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
        jdbcTemplate.update("DELETE FROM reactions WHERE target_type = 'SHORT' AND target_id = ?", SHORT_ID);
        jdbcTemplate.update("DELETE FROM shorts WHERE id = ?", SHORT_ID);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", USER_ID);
    }

    @Test
    void 동일한_좋아요_토글이_동시에_들어와도_반응과_카운트가_일치한다() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<ShortLikeResult> first = executorService.submit(() -> toggleWhenStarted(ready, start));
        Future<ShortLikeResult> second = executorService.submit(() -> toggleWhenStarted(ready, start));

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<Boolean> results = List.of(first.get().isLiked(), second.get().isLiked());
        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(count("SELECT like_count FROM shorts WHERE id = ?", SHORT_ID)).isZero();
        assertThat(count("SELECT COUNT(*) FROM reactions WHERE target_type = 'SHORT' AND target_id = ?", SHORT_ID))
                .isZero();
    }

    private ShortLikeResult toggleWhenStarted(CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("동시성 테스트 시작을 기다리다 시간이 초과되었습니다.");
        }
        return toggleShortLikeUseCase.toggle(USER_ID, SHORT_ID);
    }

    private int count(String sql, Object... args) {
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return result == null ? 0 : result;
    }
}
