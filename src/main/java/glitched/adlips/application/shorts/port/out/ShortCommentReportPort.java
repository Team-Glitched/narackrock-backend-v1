package glitched.adlips.application.shorts.port.out;

import java.util.Optional;

public interface ShortCommentReportPort {
    boolean existsByReporterAndComment(Long reporterId, Long commentId);

    Optional<Long> save(Long reporterId, Long commentId, String reason, String description);
}
