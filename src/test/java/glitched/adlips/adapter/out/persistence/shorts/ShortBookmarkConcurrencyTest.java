package glitched.adlips.adapter.out.persistence.shorts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import glitched.adlips.application.shorts.ShortBookmarkResult;
import glitched.adlips.application.shorts.ShortBookmarkApplicationException;
import glitched.adlips.application.shorts.ToggleShortBookmarkUseCase;
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
class ShortBookmarkConcurrencyTest {

    private static final long USER_ID = 903L;
    private static final long SHORT_ID = 301L;

    @Autowired ToggleShortBookmarkUseCase toggleShortBookmarkUseCase;
    @Autowired JdbcTemplate jdbcTemplate;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(2);
        cleanUpData();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (?, 'bookmark-concurrency@test.com', 'USER', CURRENT_TIMESTAMP)",
                USER_ID
        );
        jdbcTemplate.update("""
                INSERT INTO shorts (
                    id, user_id, title, media_file_id, status,
                    view_count, like_count, dislike_count, comment_count, contribution_count,
                    created_at
                ) VALUES (?, ?, 'bookmark-concurrency', 1, 'COMPLETED', 0, 0, 0, 0, 0, CURRENT_TIMESTAMP)
                """, SHORT_ID, USER_ID);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executorService.shutdownNow();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
        cleanUpData();
    }

    @Test
    void 동일한_북마크_토글이_동시에_들어와도_최종_상태가_일치한다() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<ShortBookmarkResult> first = executorService.submit(() -> toggleWhenStarted(ready, start));
        Future<ShortBookmarkResult> second = executorService.submit(() -> toggleWhenStarted(ready, start));

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<Boolean> results = List.of(first.get().isBookmarked(), second.get().isBookmarked());
        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(bookmarkCount()).isZero();
    }

    @Test
    void 완료되지_않은_숏폼은_북마크할_수_없다() {
        jdbcTemplate.update("UPDATE shorts SET status = 'IN_PROGRESS' WHERE id = ?", SHORT_ID);

        assertShortNotFound();
    }

    @Test
    void 삭제된_숏폼은_북마크할_수_없다() {
        jdbcTemplate.update("UPDATE shorts SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?", SHORT_ID);

        assertShortNotFound();
    }

    private ShortBookmarkResult toggleWhenStarted(CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("동시성 테스트 시작을 기다리다 시간이 초과되었습니다.");
        }
        return toggleShortBookmarkUseCase.toggle(USER_ID, SHORT_ID);
    }

    private int bookmarkCount() {
        Integer result = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM shorts_bookmarks WHERE user_id = ? AND shorts_id = ?",
                Integer.class,
                USER_ID,
                SHORT_ID
        );
        return result == null ? 0 : result;
    }

    private void assertShortNotFound() {
        assertThatThrownBy(() -> toggleShortBookmarkUseCase.toggle(USER_ID, SHORT_ID))
                .isInstanceOf(ShortBookmarkApplicationException.class)
                .hasMessage("숏폼을 찾을 수 없습니다.");
        assertThat(bookmarkCount()).isZero();
    }

    private void cleanUpData() {
        jdbcTemplate.update("DELETE FROM shorts_bookmarks WHERE user_id = ? AND shorts_id = ?", USER_ID, SHORT_ID);
        jdbcTemplate.update("DELETE FROM shorts WHERE id = ?", SHORT_ID);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", USER_ID);
    }
}
