package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.application.shorts.port.out.ShortReportPort;
import glitched.adlips.domain.shorts.ShortForm;
import glitched.adlips.domain.shorts.ShortReport;
import glitched.adlips.domain.user.User;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ShortReportPersistenceAdapter implements ShortReportPort {

    private final ShortFormJpaRepository shortFormRepository;
    private final ShortReportJpaRepository shortReportRepository;
    private final EntityManager entityManager;

    public ShortReportPersistenceAdapter(
            ShortFormJpaRepository shortFormRepository,
            ShortReportJpaRepository shortReportRepository,
            EntityManager entityManager
    ) {
        this.shortFormRepository = shortFormRepository;
        this.shortReportRepository = shortReportRepository;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Long> findActiveShortOwnerId(Long shortId) {
        return shortFormRepository.findActiveOwnerId(shortId);
    }

    @Override
    public boolean existsByReporterAndShort(Long reporterId, Long shortId) {
        return shortReportRepository.existsByReporterIdAndShortsId(reporterId, shortId);
    }

    @Override
    public Long save(Long reporterId, Long shortId, String reason, String description) {
        User reporter = entityManager.getReference(User.class, reporterId);
        ShortForm shorts = entityManager.getReference(ShortForm.class, shortId);
        ShortReport saved = shortReportRepository.save(new ShortReport(reporter, shorts, reason, description));
        return saved.getId();
    }
}
