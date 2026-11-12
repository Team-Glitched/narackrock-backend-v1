package glitched.adlips.adapter.out.persistence.shorts;

import static glitched.adlips.adapter.out.persistence.shorts.ShortsQueryPersistenceAdapter.VISIBLE_SHORT;

import glitched.adlips.application.shorts.ArtistAffinity;
import glitched.adlips.application.shorts.port.out.ShortRecommendationQueryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class ShortRecommendationPersistenceAdapter implements ShortRecommendationQueryPort {
    private static final String NO_PREVIOUS_INTERACTION = """
            AND s.user_id <> :viewer
            AND NOT EXISTS (
                SELECT 1 FROM shorts_bookmarks b WHERE b.user_id = :viewer AND b.shorts_id = s.id
            )
            AND NOT EXISTS (
                SELECT 1 FROM reactions r
                 WHERE r.user_id = :viewer AND r.target_type = 'SHORT' AND r.target_id = s.id
                   AND r.reaction_type IN ('LIKE', 'DISLIKE')
            )
            """;
    private final EntityManager entityManager;

    public ShortRecommendationPersistenceAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<CatalogRange> findCatalogRange() {
        String edge = """
                SELECT s.id FROM shorts s
                LEFT JOIN projects source_project ON source_project.id = s.project_id
                WHERE %s ORDER BY s.id %s LIMIT 1
                """;
        Object[] bounds = (Object[]) entityManager.createNativeQuery("SELECT ("
                + edge.formatted(VISIBLE_SHORT, "ASC") + "), ("
                + edge.formatted(VISIBLE_SHORT, "DESC") + ")").getSingleResult();
        if (bounds[0] == null) return Optional.empty();
        return Optional.of(new CatalogRange(((Number) bounds[0]).longValue(), ((Number) bounds[1]).longValue()));
    }

    @Override
    public Map<Long, ArtistAffinity> findEngagedAuthors(Long userId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT s.user_id, MAX(signal.strength)
                  FROM (
                      SELECT shorts_id AS short_id, 2 AS strength
                        FROM shorts_bookmarks WHERE user_id = :viewer
                      UNION ALL
                      SELECT target_id AS short_id, 1 AS strength
                        FROM reactions
                       WHERE user_id = :viewer AND target_type = 'SHORT' AND reaction_type = 'LIKE'
                  ) signal
                  JOIN shorts s ON s.id = signal.short_id
             LEFT JOIN projects source_project ON source_project.id = s.project_id
                 WHERE %s AND s.user_id <> :viewer
                 GROUP BY s.user_id
                """.formatted(VISIBLE_SHORT))
                .setParameter("viewer", userId)
                .getResultList();
        Map<Long, ArtistAffinity> results = new HashMap<>();
        for (Object[] row : rows) {
            results.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue() == 2
                    ? ArtistAffinity.SAVED : ArtistAffinity.LIKED);
        }
        return results;
    }

    @Override
    public List<Candidate> findForAuthors(Long userId, Set<Long> authorIds, int limit) {
        if (authorIds.isEmpty()) return List.of();
        return findCandidates(userId, authorIds, "AND s.user_id IN (:authors)", "", null, limit);
    }

    @Override
    public List<Candidate> findForDiscovery(Long userId, Set<Long> excludedAuthors, Long rotationPivot, int limit) {
        String authorFilter = excludedAuthors.isEmpty() ? "" : "AND s.user_id NOT IN (:authors)";
        if (rotationPivot == null) {
            return findCandidates(userId, excludedAuthors, authorFilter, "", null, limit);
        }
        List<Candidate> beforePivot = findCandidates(
                userId, excludedAuthors, authorFilter, "AND s.id <= :pivot", rotationPivot, limit);
        if (beforePivot.size() == limit) return beforePivot;
        List<Candidate> afterPivot = findCandidates(
                userId, excludedAuthors, authorFilter, "AND s.id > :pivot", rotationPivot, limit - beforePivot.size());
        List<Candidate> result = new ArrayList<>(beforePivot);
        result.addAll(afterPivot);
        return result;
    }

    private List<Candidate> findCandidates(
            Long userId, Set<Long> authors, String authorFilter, String rangeFilter, Long pivot, int limit
    ) {
        // Use the ID order to bound the sample before grouping songs by artist.
        Query query = entityManager.createNativeQuery("""
                SELECT id, user_id
                  FROM (
                      SELECT s.id, s.user_id
                        FROM shorts s
                        JOIN profiles p ON p.user_id = s.user_id
                        JOIN media_files media ON media.id = s.media_file_id
                   LEFT JOIN projects source_project ON source_project.id = s.project_id
                       WHERE %s %s %s %s
                       ORDER BY s.id DESC
                       LIMIT :sampleSize
                  ) candidates
                 ORDER BY ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY id DESC), id DESC
                """.formatted(VISIBLE_SHORT, userId == null ? "" : NO_PREVIOUS_INTERACTION,
                authorFilter, rangeFilter))
                .setParameter("sampleSize", Math.max(200, limit * 10))
                .setMaxResults(limit);
        if (userId != null) query.setParameter("viewer", userId);
        if (!authors.isEmpty()) query.setParameter("authors", authors);
        if (pivot != null) query.setParameter("pivot", pivot);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(row -> new Candidate(
                ((Number) row[0]).longValue(), ((Number) row[1]).longValue())).toList();
    }
}
