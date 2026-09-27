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
class PostCommentReactionPersistenceAdapterTest {

    @Autowired PostCommentJpaRepository commentRepository;
    @Autowired CommunityReactionJpaRepository reactionRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    PostCommentReactionPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PostCommentReactionPersistenceAdapter(commentRepository, reactionRepository);
        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (1, 'writer@test.com', 'USER', CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO galleries (id, name, created_at) VALUES (10, '자유 갤러리', CURRENT_TIMESTAMP)");
        insertPost(501L, "NULL");
        insertPost(502L, "CURRENT_TIMESTAMP");
        insertComment(31L, 501L, "NULL");
        insertComment(32L, 501L, "CURRENT_TIMESTAMP");
        insertComment(33L, 502L, "NULL");
        entityManager.clear();
    }

    @Test
    void 삭제되지_않은_게시글의_삭제되지_않은_댓글만_잠금_조회한다() {
        assertThat(adapter.lockActiveComment(31L)).isTrue();
        assertThat(adapter.lockActiveComment(32L)).isFalse();
        assertThat(adapter.lockActiveComment(33L)).isFalse();
        assertThat(adapter.lockActiveComment(999L)).isFalse();
    }

    @Test
    void 활성_댓글_잠금_조회는_비관적_쓰기_잠금을_사용한다() throws Exception {
        Method method = PostCommentJpaRepository.class.getDeclaredMethod("findActiveByIdForUpdate", Long.class);

        assertThat(method.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void 댓글_반응을_저장하고_변경하고_삭제한다() {
        adapter.saveReaction(1L, 31L, ReactionType.LIKE);
        flushAndClear();
        assertThat(adapter.findReaction(1L, 31L)).contains(ReactionType.LIKE);

        adapter.saveReaction(1L, 31L, ReactionType.DISLIKE);
        flushAndClear();
        assertThat(adapter.findReaction(1L, 31L)).contains(ReactionType.DISLIKE);

        adapter.deleteReaction(1L, 31L);
        flushAndClear();
        assertThat(adapter.findReaction(1L, 31L)).isEmpty();
    }

    @Test
    void 댓글_반응은_같은_id의_게시글_반응과_섞이지_않는다() {
        jdbcTemplate.update("""
                INSERT INTO reactions (user_id, target_type, target_id, reaction_type, created_at)
                VALUES (1, 'POST', 31, 'LIKE', CURRENT_TIMESTAMP)
                """);

        assertThat(adapter.findReaction(1L, 31L)).isEmpty();
    }

    @Test
    void 댓글_반응_수를_함께_조정하고_조회한다() {
        adapter.adjustReactionCounts(31L, 1, 0);
        adapter.adjustReactionCounts(31L, -1, 1);
        flushAndClear();

        assertThat(adapter.findReactionCounts(31L)).isEqualTo(new ReactionCounts(0, 1));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private void insertPost(long id, String deletedAt) {
        jdbcTemplate.update("""
                INSERT INTO posts (
                    id, gallery_id, user_id, title, content,
                    view_count, like_count, dislike_count, comment_count, deleted_at, created_at
                ) VALUES (?, 10, 1, 'title', 'content', 0, 0, 0, 0, %s, CURRENT_TIMESTAMP)
                """.formatted(deletedAt), id);
    }

    private void insertComment(long id, long postId, String deletedAt) {
        jdbcTemplate.update("""
                INSERT INTO post_comments (
                    id, post_id, user_id, content, reply_count, like_count, dislike_count, deleted_at, created_at
                ) VALUES (?, ?, 1, 'comment', 0, 0, 0, %s, CURRENT_TIMESTAMP)
                """.formatted(deletedAt), id, postId);
    }
}
