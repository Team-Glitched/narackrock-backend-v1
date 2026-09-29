package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.application.community.port.out.GalleryQueryItem;
import glitched.adlips.application.community.port.out.GalleryQueryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class GalleryQueryPersistenceAdapter implements GalleryQueryPort {

    // 구독자 수는 galleries.subscriber_count 컬럼이 아니라 gallery_subs를 세어 구한다.
    // 구독 등록 API가 아직 없어 컬럼을 증감시키는 주체가 없기 때문이다(docs/gallery-list-plan.md §4).
    private static final String SELECT_GALLERY = """
            SELECT g.id, g.name, g.description, COUNT(s.id), g.created_at
              FROM galleries g
            """;

    private static final String GROUP_AND_ORDER = """
             GROUP BY g.id, g.name, g.description, g.created_at
             ORDER BY COUNT(s.id) DESC, g.id ASC
            """;

    private final EntityManager entityManager;

    public GalleryQueryPersistenceAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<GalleryQueryItem> findGalleries() {
        Query query = entityManager.createNativeQuery(
                SELECT_GALLERY
                        + " LEFT JOIN gallery_subs s ON s.gallery_id = g.id"
                        + GROUP_AND_ORDER);
        return toQueryItems(query);
    }

    @Override
    public List<GalleryQueryItem> findSubscribedGalleries(Long userId) {
        // mine은 (user_id, gallery_id) 유니크라 갤러리당 최대 1행이다 — COUNT(s.id)가 부풀지 않는다.
        Query query = entityManager.createNativeQuery(
                        SELECT_GALLERY
                                + " JOIN gallery_subs mine ON mine.gallery_id = g.id AND mine.user_id = :userId"
                                + " LEFT JOIN gallery_subs s ON s.gallery_id = g.id"
                                + GROUP_AND_ORDER)
                .setParameter("userId", userId);
        return toQueryItems(query);
    }

    private List<GalleryQueryItem> toQueryItems(Query query) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(GalleryQueryPersistenceAdapter::toQueryItem).toList();
    }

    private static GalleryQueryItem toQueryItem(Object[] row) {
        return new GalleryQueryItem(
                ((Number) row[0]).longValue(),
                row[1].toString(),
                row[2] == null ? null : row[2].toString(),
                ((Number) row[3]).longValue(),
                localDateTime(row[4]));
    }

    private static LocalDateTime localDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        return ((Timestamp) value).toLocalDateTime();
    }
}
