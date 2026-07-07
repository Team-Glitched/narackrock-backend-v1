package glitched.adlips.application.shorts.port.out;

import java.util.Optional;

public interface ShortReportPort {
    Optional<Long> findActiveShortOwnerId(Long shortId);

    boolean existsByReporterAndShort(Long reporterId, Long shortId);

    Long save(Long reporterId, Long shortId, String reason, String description);
}
