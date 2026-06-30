package glitched.adlips.application.port;

import java.util.List;
import java.util.Set;

public interface ShortInteractionRepository {
    Set<Long> findLikedShortIds(Long userId, List<Long> shortIds);
    Set<Long> findDislikedShortIds(Long userId, List<Long> shortIds);
    Set<Long> findBookmarkedShortIds(Long userId, List<Long> shortIds);
}
