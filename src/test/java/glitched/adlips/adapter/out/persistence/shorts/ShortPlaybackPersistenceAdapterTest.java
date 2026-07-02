package glitched.adlips.adapter.out.persistence.shorts;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ShortPlaybackPersistenceAdapterTest {

    @Autowired ShortFormJpaRepository shortFormRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    ShortPlaybackPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ShortPlaybackPersistenceAdapter(shortFormRepository);
        jdbcTemplate.update("INSERT INTO users (id, email, role, created_at) VALUES (902, 'playback@test.com', 'USER', CURRENT_TIMESTAMP)");
        insertShort(201L, "COMPLETED", null);
        insertShort(202L, "IN_PROGRESS", null);
        insertShort(203L, "COMPLETED", "CURRENT_TIMESTAMP");
    }

    @Test
    void 완료되고_삭제되지_않은_숏폼만_활성_상태로_조회한다() {
        assertThat(adapter.existsActiveShort(201L)).isTrue();
        assertThat(adapter.existsActiveShort(202L)).isFalse();
        assertThat(adapter.existsActiveShort(203L)).isFalse();
        assertThat(adapter.existsActiveShort(999L)).isFalse();
    }

    private void insertShort(long id, String status, String deletedAtExpression) {
        String deletedAt = deletedAtExpression == null ? "NULL" : deletedAtExpression;
        jdbcTemplate.update("""
                INSERT INTO shorts (
                    id, user_id, title, media_file_id, status,
                    view_count, like_count, dislike_count, comment_count, contribution_count,
                    deleted_at, created_at
                ) VALUES (?, 902, 'test', 1, ?, 0, 0, 0, 0, 0, %s, CURRENT_TIMESTAMP)
                """.formatted(deletedAt), id, status);
        entityManager.clear();
    }
}
