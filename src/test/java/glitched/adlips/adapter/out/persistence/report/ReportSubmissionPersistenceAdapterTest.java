package glitched.adlips.adapter.out.persistence.report;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.report.port.out.ReportTarget;
import glitched.adlips.domain.report.ReportTargetType;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ReportSubmissionPersistenceAdapterTest {

    @Autowired ReportJpaRepository reportRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    ReportSubmissionPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ReportSubmissionPersistenceAdapter(reportRepository, entityManager);
        jdbcTemplate.update("""
                INSERT INTO users (id, email, role, created_at)
                VALUES
                    (1, 'reporter@test.com', 'USER', CURRENT_TIMESTAMP),
                    (2, 'owner@test.com', 'USER', CURRENT_TIMESTAMP),
                    (3, 'admin@test.com', 'ADMIN', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("INSERT INTO galleries (id, name, created_at) VALUES (1, 'gallery', CURRENT_TIMESTAMP)");
        jdbcTemplate.update("""
                INSERT INTO posts (
                    id, gallery_id, user_id, title, content,
                    view_count, like_count, dislike_count, comment_count,
                    deleted_at, created_at
                ) VALUES
                    (10, 1, 2, 'active', 'body', 0, 0, 0, 1, NULL, CURRENT_TIMESTAMP),
                    (11, 1, 2, 'deleted', 'body', 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO post_comments (
                    id, post_id, user_id, content, reply_count, deleted_at, created_at
                ) VALUES
                    (20, 10, 2, 'active comment', 0, NULL, CURRENT_TIMESTAMP),
                    (21, 10, 2, 'deleted comment', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                    (22, 11, 2, 'orphaned comment', 0, NULL, CURRENT_TIMESTAMP)
                """);
        entityManager.clear();
    }

    @Test
    void 활성_게시글과_댓글의_작성자를_조회한다() {
        assertThat(adapter.findActiveTarget(ReportTargetType.POST, 10L))
                .contains(new ReportTarget(2L));
        assertThat(adapter.findActiveTarget(ReportTargetType.POST_COMMENT, 20L))
                .contains(new ReportTarget(2L));
    }

    @Test
    void 삭제된_대상이나_삭제된_게시글의_댓글은_조회하지_않는다() {
        assertThat(adapter.findActiveTarget(ReportTargetType.POST, 11L)).isEmpty();
        assertThat(adapter.findActiveTarget(ReportTargetType.POST_COMMENT, 21L)).isEmpty();
        assertThat(adapter.findActiveTarget(ReportTargetType.POST_COMMENT, 22L)).isEmpty();
    }

    @Test
    void 신고를_저장하고_중복을_감지한다() {
        Long reportId = adapter.save(
                1L, ReportTargetType.POST, 10L, "욕설/비방", "상세"
        ).orElseThrow();

        assertThat(reportId).isNotNull();
        assertThat(adapter.exists(1L, ReportTargetType.POST, 10L)).isTrue();
        assertThat(adapter.save(
                1L, ReportTargetType.POST, 10L, "스팸", null
        )).isEmpty();
    }

    @Test
    void 현재_유효한_이용_정지를_조회한다() {
        jdbcTemplate.update("""
                INSERT INTO user_bans (
                    id, user_id, admin_id, reason, banned_until, lifted_at, created_at
                ) VALUES (30, 1, 3, 'policy', '2026-09-22 00:00:00', NULL, CURRENT_TIMESTAMP)
                """);

        assertThat(adapter.isBanned(1L, LocalDateTime.of(2026, 9, 21, 3, 0))).isTrue();
        assertThat(adapter.isBanned(1L, LocalDateTime.of(2026, 9, 23, 3, 0))).isFalse();
    }
}
