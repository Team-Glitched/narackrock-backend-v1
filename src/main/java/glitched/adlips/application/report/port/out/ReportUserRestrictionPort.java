package glitched.adlips.application.report.port.out;

import java.time.LocalDateTime;

public interface ReportUserRestrictionPort {
    boolean isBanned(Long userId, LocalDateTime now);
}
