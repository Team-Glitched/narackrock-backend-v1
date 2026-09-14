package glitched.adlips.application.shorts.port.out;

import glitched.adlips.application.shorts.ShortReactionCounts;
import java.util.List;
import java.util.Map;

public interface ShortReactionCountCachePort {

    Map<Long, ShortReactionCounts> findAll(List<Long> shortIds);

    void put(Long shortId, ShortReactionCounts counts);
}
