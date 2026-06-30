package glitched.adlips.application.port;

import glitched.adlips.domain.shorts.ShortParticipant;
import java.util.List;

public interface ShortParticipantRepository {
    List<ShortParticipant> findByShortIds(List<Long> shortIds);
}
