package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.domain.shorts.ShortReport;
import org.springframework.data.jpa.repository.JpaRepository;

interface ShortReportJpaRepository extends JpaRepository<ShortReport, Long> {
    boolean existsByReporterIdAndShortsId(Long reporterId, Long shortsId);
}
