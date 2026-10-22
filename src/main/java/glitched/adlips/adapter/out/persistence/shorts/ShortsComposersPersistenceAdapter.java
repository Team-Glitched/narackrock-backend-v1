package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.application.shorts.port.out.ShortsComposerQueryItem;
import glitched.adlips.application.shorts.port.out.ShortsComposersQueryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class ShortsComposersPersistenceAdapter implements ShortsComposersQueryPort {

    private final ShortFormJpaRepository shortFormRepository;
    private final EntityManager entityManager;

    public ShortsComposersPersistenceAdapter(
            ShortFormJpaRepository shortFormRepository,
            EntityManager entityManager
    ) {
        this.shortFormRepository = shortFormRepository;
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsActiveShort(Long shortId) {
        return shortFormRepository.existsByIdAndDeletedAtIsNull(shortId);
    }

    @Override
    public List<ShortsComposerQueryItem> findByShortId(Long shortId) {
        Query query = entityManager.createNativeQuery("""
                SELECT sp.user_id, p.nickname, mf.file_url, sp.role, sp.description
                  FROM shorts_participants sp
                  JOIN profiles p ON p.user_id = sp.user_id
             LEFT JOIN media_files mf ON mf.id = p.profile_image_file_id
                 WHERE sp.shorts_id = :shortId
                 ORDER BY sp.id ASC
                """)
                .setParameter("shortId", shortId);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        Map<Long, Set<String>> instruments = new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
        List<Object[]> trackRows = entityManager.createNativeQuery("""
                SELECT t.owner_id, t.instrument FROM project_tracks t
                  JOIN shorts s ON s.project_id = t.project_id
                 WHERE s.id = :shortId AND t.approval_status = 'APPROVED' AND t.is_deleted = false
                 ORDER BY t.sort_order, t.id
                """).setParameter("shortId", shortId).getResultList();
        for (Object[] track : trackRows) {
            String instrument = stringValue(track[1]);
            instruments.computeIfAbsent(longValue(track[0]), ignored -> new LinkedHashSet<>())
                    .add(instrument == null || instrument.isBlank() ? "AUDIO" : instrument);
        }
        Map<Long, String> descriptions = new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
        List<Object[]> contributionRows = entityManager.createNativeQuery("""
                SELECT c.user_id, c.description FROM project_contributions c
                  JOIN shorts s ON s.project_id = c.project_id
                 WHERE s.id = :shortId AND c.approval_status = 'APPROVED'
                 ORDER BY c.id DESC
                """).setParameter("shortId", shortId).getResultList();
        for (Object[] contribution : contributionRows) {
            Long userId = longValue(contribution[0]);
            // Even an empty latest description supersedes an older contribution's text.
            if (!descriptions.containsKey(userId)) descriptions.put(userId, stringValue(contribution[1]));
        }
        return rows.stream()
                .map(row -> new ShortsComposerQueryItem(
                        longValue(row[0]), stringValue(row[1]), stringValue(row[2]),
                        instruments.containsKey(longValue(row[0]))
                                ? String.join(", ", instruments.get(longValue(row[0]))) : stringValue(row[3]),
                        descriptions.containsKey(longValue(row[0]))
                                ? descriptions.get(longValue(row[0])) : stringValue(row[4])))
                .toList();
    }

    private static Long longValue(Object value) {
        return ((Number) value).longValue();
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
