package glitched.adlips.adapter.out.persistence.report;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.domain.report.ModerationAction;
import glitched.adlips.domain.report.ModerationActionType;
import glitched.adlips.domain.report.Report;
import glitched.adlips.domain.report.ReportStatus;
import glitched.adlips.domain.report.ReportTargetType;
import glitched.adlips.domain.user.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ReportResolutionPersistenceAdapterTest {

    @Autowired ReportJpaRepository reportRepository;
    @Autowired ModerationActionJpaRepository moderationActionRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    ReportResolutionPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ReportResolutionPersistenceAdapter(reportRepository, moderationActionRepository);

        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (1, 'reporter@test.com', 'USER', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (2, 'admin@test.com', 'ADMIN', CURRENT_TIMESTAMP)");
        jdbcTemplate.update("""
                INSERT INTO reports (id, reporter_id, target_type, target_id, reason, status, created_at)
                VALUES (101, 1, 'SHORT', 12, 'COPYRIGHT', 'PENDING', CURRENT_TIMESTAMP)
                """);
        entityManager.clear();
    }

    @Test
    void 존재하는_신고를_id로_조회한다() {
        Optional<Report> found = adapter.findByIdForUpdate(101L);

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(ReportStatus.PENDING);
    }

    @Test
    void 존재하지_않는_신고는_빈_값을_반환한다() {
        assertThat(adapter.findByIdForUpdate(999L)).isEmpty();
    }

    @Test
    void 신고를_저장하면_변경된_상태가_반영된다() {
        Report report = adapter.findByIdForUpdate(101L).orElseThrow();
        User admin = entityManager.getReference(User.class, 2L);
        report.resolve(admin, ReportStatus.RESOLVED, LocalDateTime.now());

        adapter.save(report);
        entityManager.flush();
        entityManager.clear();

        Report reloaded = reportRepository.findById(101L).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(reloaded.getHandledAt()).isNotNull();
    }

    @Test
    void moderation_action을_저장한다() {
        Report report = adapter.findByIdForUpdate(101L).orElseThrow();
        User admin = entityManager.getReference(User.class, 2L);

        adapter.saveModerationAction(new ModerationAction(
                report, admin, ReportTargetType.SHORT, 12L, ModerationActionType.HIDE_CONTENT, "운영 정책 위반"));
        entityManager.flush();

        assertThat(moderationActionRepository.findAll()).hasSize(1);
        assertThat(moderationActionRepository.findAll().get(0).getActionType())
                .isEqualTo(ModerationActionType.HIDE_CONTENT);
    }
}
