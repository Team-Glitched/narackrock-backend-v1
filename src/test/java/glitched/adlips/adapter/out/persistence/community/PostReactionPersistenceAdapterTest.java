package glitched.adlips.adapter.out.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.community.port.out.ReactionCounts;
import glitched.adlips.domain.reaction.ReactionType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class PostReactionPersistenceAdapterTest {

    @Autowired PostJpaRepository postRepository;
    @Autowired CommunityReactionJpaRepository reactionRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    PostReactionPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PostReactionPersistenceAdapter(postRepository, reactionRepository);
        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (1, 'writer@test.com', 'USER', CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO galleries (id, name, created_at) VALUES (10, '자유 갤러리', CURRENT_TIMESTAMP)");
        insertPost(501L, null);
        insertPost(502L, "CURRENT_TIMESTAMP");
        entityManager.clear();
    }

    @Test
    void 삭제되지_않은_게시글만_잠금_조회한다() {
        assertThat(adapter.lockActivePost(501L)).isTrue();
        assertThat(adapter.lockActivePost(502L)).isFalse();
        assertThat(adapter.lockActivePost(999L)).isFalse();
    }

    @Test
    void 활성_게시글_잠금_조회는_비관적_쓰기_잠금을_사용한다() throws Exception {
        Method method = PostJpaRepository.class.getDeclaredMethod("findActiveByIdForUpdate", Long.class);

        assertThat(method.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void 반응을_저장하고_변경하고_삭제한다() {
        adapter.saveReaction(1L, 501L, ReactionType.LIKE);
        flushAndClear();
        assertThat(adapter.findReaction(1L, 501L)).contains(ReactionType.LIKE);

        adapter.saveReaction(1L, 501L, ReactionType.DISLIKE);
        flushAndClear();
        assertThat(adapter.findReaction(1L, 501L)).contains(ReactionType.DISLIKE);

        adapter.deleteReaction(1L, 501L);
        flushAndClear();
        assertThat(adapter.findReaction(1L, 501L)).isEmpty();
    }

    @Test
    void 게시글_반응은_같은_id의_다른_대상_반응과_섞이지_않는다() {
        jdbcTemplate.update("""
                INSERT INTO reactions (user_id, target_type, target_id, reaction_type, created_at)
                VALUES (1, 'SHORT', 501, 'LIKE', CURRENT_TIMESTAMP)
                """);

        assertThat(adapter.findReaction(1L, 501L)).isEmpty();
    }

    @Test
    void 반응_수를_함께_조정하고_조회한다() {
        adapter.adjustReactionCounts(501L, 1, 0);
        adapter.adjustReactionCounts(501L, -1, 1);
        adapter.adjustReactionCounts(501L, 1, 0);
        flushAndClear();

        assertThat(adapter.findReactionCounts(501L)).isEqualTo(new ReactionCounts(1, 1));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private void insertPost(long id, String deletedAtExpression) {
        String deletedAt = deletedAtExpression == null ? "NULL" : deletedAtExpression;
        jdbcTemplate.update("""
                INSERT INTO posts (
                    id, gallery_id, user_id, title, content,
                    view_count, like_count, dislike_count, comment_count, deleted_at, created_at
                ) VALUES (?, 10, 1, 'title', 'content', 0, 0, 0, 0, %s, CURRENT_TIMESTAMP)
                """.formatted(deletedAt), id);
    }
}
