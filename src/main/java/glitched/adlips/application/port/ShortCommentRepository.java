package glitched.adlips.application.port;

import java.util.List;
import java.util.Map;

public interface ShortCommentRepository {
    Map<Long, Long> countByShortIds(List<Long> shortIds);
}
