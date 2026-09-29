package glitched.adlips.application.report.port.out;

import glitched.adlips.domain.report.ReportTargetType;
import java.util.Optional;

public interface ReportSubmissionPort {
    boolean exists(Long reporterId, ReportTargetType targetType, Long targetId);

    Optional<Long> save(
            Long reporterId,
            ReportTargetType targetType,
            Long targetId,
            String reason,
            String description
    );
}
