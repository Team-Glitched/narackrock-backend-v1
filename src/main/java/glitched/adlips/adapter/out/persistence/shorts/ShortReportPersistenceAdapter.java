package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.adapter.out.persistence.report.ReportJpaRepository;
import glitched.adlips.application.shorts.port.out.ShortReportPort;
import glitched.adlips.domain.report.Report;
import glitched.adlips.domain.report.ReportTargetType;
import glitched.adlips.domain.user.User;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class ShortReportPersistenceAdapter implements ShortReportPort {

    private final ShortFormJpaRepository shortFormRepository;
    private final ReportJpaRepository reportRepository;
    private final EntityManager entityManager;

    public ShortReportPersistenceAdapter(
            ShortFormJpaRepository shortFormRepository,
            ReportJpaRepository reportRepository,
            EntityManager entityManager
    ) {
        this.shortFormRepository = shortFormRepository;
        this.reportRepository = reportRepository;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Long> findActiveShortOwnerId(Long shortId) {
        return shortFormRepository.findActiveOwnerId(shortId);
    }

    @Override
    public boolean existsByReporterAndShort(Long reporterId, Long shortId) {
        return reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                reporterId, ReportTargetType.SHORT, shortId);
    }

    @Override
    public Optional<Long> save(Long reporterId, Long shortId, String reason, String description) {
        User reporter = entityManager.getReference(User.class, reporterId);
        try {
            Report saved = reportRepository.saveAndFlush(
                    new Report(reporter, ReportTargetType.SHORT, shortId, reason, description));
            return Optional.of(saved.getId());
        } catch (DataIntegrityViolationException exception) {
            return Optional.empty();
        }
    }
}
