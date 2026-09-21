package glitched.adlips.application.report.port.out;

import glitched.adlips.domain.report.ReportTargetType;
import java.util.Optional;

public interface ReportTargetQueryPort {
    Optional<ReportTarget> findActiveTarget(ReportTargetType targetType, Long targetId);
}
