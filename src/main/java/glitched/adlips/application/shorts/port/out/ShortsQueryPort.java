package glitched.adlips.application.shorts.port.out;

import glitched.adlips.application.shorts.ShortsSource;
import glitched.adlips.domain.shorts.ShortStatus;
import java.util.List;

public interface ShortsQueryPort {
    List<ShortsQueryItem> findByCursor(
            Long cursor,
            ShortsSource source,
            ShortStatus status,
            int limit,
            Long currentUserId
    );

    List<ShortsParticipantQueryItem> findParticipants(List<Long> shortIds);
}
