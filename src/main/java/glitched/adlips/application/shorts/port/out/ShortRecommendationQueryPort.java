package glitched.adlips.application.shorts.port.out;

import glitched.adlips.application.shorts.RecommendationReason;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ShortRecommendationQueryPort {
    record Candidate(Long shortId, Long authorId) {}

    record CatalogRange(long oldestId, long newestId) {}

    Map<Long, RecommendationReason> findEngagedAuthors(Long userId);

    List<Candidate> findForAuthors(Long userId, Set<Long> authorIds, int limit);

    List<Candidate> findForDiscovery(Long userId, Set<Long> excludedAuthors, Long rotationPivot, int limit);

    Optional<CatalogRange> findCatalogRange();
}
