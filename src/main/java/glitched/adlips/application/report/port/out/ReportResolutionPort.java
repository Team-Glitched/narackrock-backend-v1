package glitched.adlips.application.report.port.out;

import glitched.adlips.domain.report.ModerationAction;
import glitched.adlips.domain.report.Report;
import java.util.Optional;

public interface ReportResolutionPort {
    Optional<Report> findByIdForUpdate(Long reportId);

    void save(Report report);

    void saveModerationAction(ModerationAction action);
}
