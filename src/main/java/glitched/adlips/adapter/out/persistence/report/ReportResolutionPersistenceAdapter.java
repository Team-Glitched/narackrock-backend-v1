package glitched.adlips.adapter.out.persistence.report;

import glitched.adlips.application.report.port.out.ReportResolutionPort;
import glitched.adlips.domain.report.ModerationAction;
import glitched.adlips.domain.report.Report;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ReportResolutionPersistenceAdapter implements ReportResolutionPort {

    private final ReportJpaRepository reportRepository;
    private final ModerationActionJpaRepository moderationActionRepository;

    public ReportResolutionPersistenceAdapter(
            ReportJpaRepository reportRepository,
            ModerationActionJpaRepository moderationActionRepository
    ) {
        this.reportRepository = reportRepository;
        this.moderationActionRepository = moderationActionRepository;
    }

    @Override
    public Optional<Report> findByIdForUpdate(Long reportId) {
        return reportRepository.findByIdForUpdate(reportId);
    }

    @Override
    public void save(Report report) {
        reportRepository.save(report);
    }

    @Override
    public void saveModerationAction(ModerationAction action) {
        moderationActionRepository.save(action);
    }
}
