package glitched.adlips.adapter.out.persistence.shorts;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.adapter.out.persistence.report.ReportJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ShortCommentReportPersistenceAdapterTest {

    @Autowired ReportJpaRepository reportRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    ShortCommentReportPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ShortCommentReportPersistenceAdapter(reportRepository, entityManager);

        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (1, 'reporter@test.com', 'USER', CURRENT_TIMESTAMP)");
    }

    @Test
    void 신고_기록이_없으면_false를_반환한다() {
        assertThat(adapter.existsByReporterAndComment(1L, 7721L)).isFalse();
    }

    @Test
    void 저장하면_생성된_id를_반환하고_이후_중복_조회에서_true를_반환한다() {
        Long reportId = adapter.save(1L, 7721L, "욕설/비방", "특정 사용자를 비방하는 내용이 포함되어 있습니다.")
                .orElseThrow();
        entityManager.flush();

        assertThat(reportId).isNotNull();
        assertThat(adapter.existsByReporterAndComment(1L, 7721L)).isTrue();
    }

    @Test
    void 같은_신고자와_댓글_조합으로_두_번_저장하면_빈_값을_반환한다() {
        adapter.save(1L, 7721L, "욕설/비방", "첫 번째 신고");

        assertThat(adapter.save(1L, 7721L, "스팸", "두 번째 신고")).isEmpty();
    }
}
