package glitched.adlips.adapter.out.persistence.shorts;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ShortsCompositionDetailPersistenceAdapterTest {

    @Autowired ShortFormJpaRepository shortFormRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    ShortsCompositionDetailPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ShortsCompositionDetailPersistenceAdapter(shortFormRepository);
        jdbcTemplate.update("INSERT INTO users (id, email, role, created_at) VALUES (902, 'author@test.com', 'USER', CURRENT_TIMESTAMP)");
        insertShort(201L, "밤하늘 위 멜로디", null);
        insertShort(202L, "삭제됨", "CURRENT_TIMESTAMP");
    }

    @Test
    void 존재하고_삭제되지_않은_숏폼은_제목을_반환한다() {
        Optional<String> result = adapter.findActiveShortTitle(201L);

        assertThat(result).contains("밤하늘 위 멜로디");
    }

    @Test
    void 삭제된_숏폼은_빈_값을_반환한다() {
        assertThat(adapter.findActiveShortTitle(202L)).isEmpty();
    }

    @Test
    void 존재하지_않는_숏폼은_빈_값을_반환한다() {
        assertThat(adapter.findActiveShortTitle(999L)).isEmpty();
    }

    private void insertShort(long id, String title, String deletedAtExpression) {
        String deletedAt = deletedAtExpression == null ? "NULL" : deletedAtExpression;
        jdbcTemplate.update("""
                INSERT INTO shorts (
                    id, user_id, title, media_file_id, status,
                    view_count, like_count, dislike_count, comment_count, contribution_count,
                    deleted_at, created_at
                ) VALUES (?, 902, ?, 1, 'IN_PROGRESS', 0, 0, 0, 0, 0, %s, CURRENT_TIMESTAMP)
                """.formatted(deletedAt), id, title);
        entityManager.clear();
    }
}
