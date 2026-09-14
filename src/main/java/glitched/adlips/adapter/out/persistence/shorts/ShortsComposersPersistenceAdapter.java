package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.application.shorts.port.out.ShortsComposerQueryItem;
import glitched.adlips.application.shorts.port.out.ShortsComposersQueryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
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
        return rows.stream()
                .map(row -> new ShortsComposerQueryItem(
                        longValue(row[0]), stringValue(row[1]), stringValue(row[2]),
                        stringValue(row[3]), stringValue(row[4])))
                .toList();
    }

    private static Long longValue(Object value) {
        return ((Number) value).longValue();
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
