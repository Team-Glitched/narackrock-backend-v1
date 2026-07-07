package glitched.adlips.adapter.out.persistence.report;

import glitched.adlips.domain.report.Report;
import glitched.adlips.domain.report.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportJpaRepository extends JpaRepository<Report, Long> {
    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);
}
