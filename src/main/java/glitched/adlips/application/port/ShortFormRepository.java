package glitched.adlips.application.port;

import glitched.adlips.domain.shorts.ShortForm;
import glitched.adlips.domain.shorts.ShortStatus;
import java.util.List;

public interface ShortFormRepository {
    List<ShortForm> findByCursor(Long cursor, ShortStatus status, int limit);
}
