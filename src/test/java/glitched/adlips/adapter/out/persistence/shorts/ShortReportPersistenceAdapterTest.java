package glitched.adlips.adapter.out.persistence.shorts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ShortReportPersistenceAdapterTest {

    @Autowired ShortFormJpaRepository shortFormRepository;
    @Autowired ShortReportJpaRepository shortReportRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    ShortReportPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ShortReportPersistenceAdapter(shortFormRepository, shortReportRepository, entityManager);

        jdbcTemplate.update("INSERT INTO users (id, email, role, created_at) VALUES (1, 'reporter@test.com', 'USER', CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO users (id, email, role, created_at) VALUES (99, 'owner@test.com', 'USER', CURRENT_TIMESTAMP)");
        insertShort(201L, 99L, null);
        insertShort(202L, 99L, "CURRENT_TIMESTAMP");
    }

    @Test
    void 존재하고_삭제되지_않은_숏폼의_소유자_id를_반환한다() {
        assertThat(adapter.findActiveShortOwnerId(201L)).contains(99L);
    }

    @Test
    void 삭제된_숏폼은_빈_값을_반환한다() {
        assertThat(adapter.findActiveShortOwnerId(202L)).isEmpty();
    }

    @Test
    void 존재하지_않는_숏폼은_빈_값을_반환한다() {
        assertThat(adapter.findActiveShortOwnerId(999L)).isEmpty();
    }

    @Test
    void 신고_기록이_없으면_false를_반환한다() {
        assertThat(adapter.existsByReporterAndShort(1L, 201L)).isFalse();
    }

    @Test
    void 저장하면_생성된_id를_반환하고_이후_중복_조회에서_true를_반환한다() {
        Long reportId = adapter.save(1L, 201L, "COPYRIGHT", "저작권 침해가 의심됩니다.");
        entityManager.flush();

        assertThat(reportId).isNotNull();
        assertThat(adapter.existsByReporterAndShort(1L, 201L)).isTrue();
    }

    @Test
    void 같은_신고자와_숏폼_조합으로_두_번_저장하면_유니크_제약_위반이_발생한다() {
        adapter.save(1L, 201L, "COPYRIGHT", "첫 번째 신고");
        entityManager.flush();

        assertThatThrownBy(() -> {
            adapter.save(1L, 201L, "SPAM", "두 번째 신고");
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertShort(long id, long ownerId, String deletedAtExpression) {
        String deletedAt = deletedAtExpression == null ? "NULL" : deletedAtExpression;
        jdbcTemplate.update("""
                INSERT INTO shorts (
                    id, user_id, title, media_file_id, status,
                    view_count, like_count, dislike_count, comment_count, contribution_count,
                    deleted_at, created_at
                ) VALUES (?, ?, 'test', 1, 'IN_PROGRESS', 0, 0, 0, 0, 0, %s, CURRENT_TIMESTAMP)
                """.formatted(deletedAt), id, ownerId);
        entityManager.clear();
    }
}
