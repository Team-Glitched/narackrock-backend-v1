package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.adapter.out.persistence.report.ReportJpaRepository;
import glitched.adlips.application.shorts.port.out.ShortCommentReportPort;
import glitched.adlips.domain.report.Report;
import glitched.adlips.domain.report.ReportTargetType;
import glitched.adlips.domain.user.User;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class ShortCommentReportPersistenceAdapter implements ShortCommentReportPort {

    private final ReportJpaRepository reportRepository;
    private final EntityManager entityManager;

    public ShortCommentReportPersistenceAdapter(
            ReportJpaRepository reportRepository,
            EntityManager entityManager
    ) {
        this.reportRepository = reportRepository;
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsByReporterAndComment(Long reporterId, Long commentId) {
        return reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                reporterId, ReportTargetType.SHORT_COMMENT, commentId);
    }

    @Override
    public Optional<Long> save(Long reporterId, Long commentId, String reason, String description) {
        User reporter = entityManager.getReference(User.class, reporterId);
        try {
            Report saved = reportRepository.saveAndFlush(
                    new Report(reporter, ReportTargetType.SHORT_COMMENT, commentId, reason, description));
            return Optional.of(saved.getId());
        } catch (DataIntegrityViolationException exception) {
            return Optional.empty();
        }
    }
}
