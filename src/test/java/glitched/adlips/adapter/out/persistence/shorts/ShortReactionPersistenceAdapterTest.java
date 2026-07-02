package glitched.adlips.adapter.out.persistence.shorts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import glitched.adlips.domain.reaction.Reaction;
import glitched.adlips.domain.reaction.ReactionTargetType;
import glitched.adlips.domain.reaction.ReactionType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ShortReactionPersistenceAdapterTest {

    @Autowired ReactionJpaRepository reactionRepository;
    @Autowired ShortFormJpaRepository shortFormRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO users (id, email, role, created_at) VALUES (901, 'reaction@test.com', 'USER', CURRENT_TIMESTAMP)");
        insertShort(101L, "COMPLETED", null);
        insertShort(102L, "IN_PROGRESS", null);
        insertShort(103L, "COMPLETED", "CURRENT_TIMESTAMP");
    }

    @Test
    void 한_사용자는_같은_대상에_반응_하나만_저장할_수_있다() {
        reactionRepository.saveAndFlush(Reaction.of(
                901L, ReactionTargetType.SHORT, 101L, ReactionType.LIKE));

        assertThatThrownBy(() -> reactionRepository.saveAndFlush(Reaction.of(
                901L, ReactionTargetType.SHORT, 101L, ReactionType.DISLIKE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 완료되고_삭제되지_않은_숏폼만_잠금_조회한다() {
        assertThat(shortFormRepository.findActiveByIdForUpdate(101L)).isPresent();
        assertThat(shortFormRepository.findActiveByIdForUpdate(102L)).isEmpty();
        assertThat(shortFormRepository.findActiveByIdForUpdate(103L)).isEmpty();
        assertThat(shortFormRepository.findActiveByIdForUpdate(999L)).isEmpty();
    }

    @Test
    void 활성_숏폼_조회는_비관적_쓰기_잠금을_사용한다() throws Exception {
        Method method = ShortFormJpaRepository.class
                .getDeclaredMethod("findActiveByIdForUpdate", Long.class);

        assertThat(method.getAnnotation(Lock.class).value())
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    private void insertShort(long id, String status, String deletedAtExpression) {
        String deletedAt = deletedAtExpression == null ? "NULL" : deletedAtExpression;
        jdbcTemplate.update("""
                INSERT INTO shorts (
                    id, user_id, title, media_file_id, status,
                    view_count, like_count, dislike_count, comment_count, contribution_count,
                    deleted_at, created_at
                ) VALUES (?, 901, 'test', 1, ?, 0, 0, 0, 0, 0, %s, CURRENT_TIMESTAMP)
                """.formatted(deletedAt), id, status);
        entityManager.clear();
    }
}
