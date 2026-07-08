package glitched.adlips.adapter.out.persistence.report;

import glitched.adlips.domain.report.ModerationAction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationActionJpaRepository extends JpaRepository<ModerationAction, Long> {
}
