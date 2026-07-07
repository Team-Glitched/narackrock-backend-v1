package glitched.adlips.adapter.out.persistence.shorts;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ShortsSharePersistenceAdapterTest {

    @Autowired ShortFormJpaRepository shortFormRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    ShortsSharePersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ShortsSharePersistenceAdapter(shortFormRepository);
        jdbcTemplate.update("INSERT INTO users (id, email, role, created_at) VALUES (902, 'author@test.com', 'USER', CURRENT_TIMESTAMP)");
        insertShort(201L, null);
        insertShort(202L, "CURRENT_TIMESTAMP");
    }

    @Test
    void 존재하고_삭제되지_않은_숏폼은_true다() {
        assertThat(adapter.existsActiveShort(201L)).isTrue();
    }

    @Test
    void 삭제된_숏폼은_false다() {
        assertThat(adapter.existsActiveShort(202L)).isFalse();
    }

    @Test
    void 존재하지_않는_숏폼은_false다() {
        assertThat(adapter.existsActiveShort(999L)).isFalse();
    }

    private void insertShort(long id, String deletedAtExpression) {
        String deletedAt = deletedAtExpression == null ? "NULL" : deletedAtExpression;
        jdbcTemplate.update("""
                INSERT INTO shorts (
                    id, user_id, title, media_file_id, status,
                    view_count, like_count, dislike_count, comment_count, contribution_count,
                    deleted_at, created_at
                ) VALUES (?, 902, 'test', 1, 'IN_PROGRESS', 0, 0, 0, 0, 0, %s, CURRENT_TIMESTAMP)
                """.formatted(deletedAt), id);
        entityManager.clear();
    }
}
