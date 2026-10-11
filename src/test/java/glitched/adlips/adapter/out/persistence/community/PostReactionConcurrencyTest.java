package glitched.adlips.adapter.out.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.community.PostReactionResult;
import glitched.adlips.application.community.TogglePostReactionUseCase;
import glitched.adlips.domain.reaction.ReactionType;
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
class PostReactionConcurrencyTest {

    private static final long USER_ID = 904L;
    private static final long GALLERY_ID = 904L;
    private static final long POST_ID = 904L;

    @Autowired TogglePostReactionUseCase togglePostReactionUseCase;
    @Autowired JdbcTemplate jdbcTemplate;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(2);
        cleanUpData();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (?, 'post-concurrency@test.com', 'USER', CURRENT_TIMESTAMP)",
                USER_ID);
        jdbcTemplate.update(
                "INSERT INTO galleries (id, name, created_at) VALUES (?, 'post-concurrency', CURRENT_TIMESTAMP)",
                GALLERY_ID);
        jdbcTemplate.update("""
                INSERT INTO posts (
                    id, gallery_id, user_id, title, content,
                    view_count, like_count, dislike_count, comment_count, created_at
                ) VALUES (?, ?, ?, 'title', 'content', 0, 0, 0, 0, CURRENT_TIMESTAMP)
                """, POST_ID, GALLERY_ID, USER_ID);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executorService.shutdownNow();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
        cleanUpData();
    }

    @Test
    void 동일한_좋아요_토글이_동시에_들어와도_반응과_카운트가_일치한다() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<PostReactionResult> first = executorService.submit(() -> toggleWhenStarted(ready, start));
        Future<PostReactionResult> second = executorService.submit(() -> toggleWhenStarted(ready, start));

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<Boolean> results = List.of(first.get().liked(), second.get().liked());
        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(count("SELECT like_count FROM posts WHERE id = ?", POST_ID)).isZero();
        assertThat(count("SELECT COUNT(*) FROM reactions WHERE target_type = 'POST' AND target_id = ?", POST_ID))
                .isZero();
    }

    private PostReactionResult toggleWhenStarted(CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("동시성 테스트 시작을 기다리다 시간이 초과되었습니다.");
        }
        return togglePostReactionUseCase.toggle(USER_ID, POST_ID, ReactionType.LIKE);
    }

    private void cleanUpData() {
        jdbcTemplate.update("DELETE FROM reactions WHERE target_type = 'POST' AND target_id = ?", POST_ID);
        jdbcTemplate.update("DELETE FROM posts WHERE id = ?", POST_ID);
        jdbcTemplate.update("DELETE FROM galleries WHERE id = ?", GALLERY_ID);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", USER_ID);
    }

    private int count(String sql, Object... args) {
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return result == null ? 0 : result;
    }
}
