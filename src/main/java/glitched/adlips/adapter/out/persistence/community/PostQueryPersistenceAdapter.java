package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.application.community.port.out.PostQueryItem;
import glitched.adlips.application.community.port.out.PostQueryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PostQueryPersistenceAdapter implements PostQueryPort {

    private static final String SELECT_POST = """
            SELECT p.id, p.gallery_id, p.user_id, pr.nickname, media.file_url,
                   p.title, p.content, p.view_count, p.like_count, p.dislike_count, p.comment_count, p.created_at
              FROM posts p
              JOIN profiles pr ON pr.user_id = p.user_id
         LEFT JOIN media_files media ON media.id = pr.profile_image_file_id
            """;

    private final GalleryJpaRepository galleryRepository;
    private final EntityManager entityManager;

    public PostQueryPersistenceAdapter(GalleryJpaRepository galleryRepository, EntityManager entityManager) {
        this.galleryRepository = galleryRepository;
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsGallery(Long galleryId) {
        return galleryRepository.existsById(galleryId);
    }

    @Override
    public Optional<PostQueryItem> findDetail(Long postId, Long galleryId) {
        String sql = SELECT_POST + " WHERE p.id = :postId AND p.gallery_id = :galleryId AND p.deleted_at IS NULL";
        Query query = entityManager.createNativeQuery(sql)
                .setParameter("postId", postId)
                .setParameter("galleryId", galleryId);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().findFirst().map(this::toQueryItem);
    }

    @Override
    public List<PostQueryItem> findPosts(Long galleryId, int page, int size) {
        String sql = SELECT_POST
                + " WHERE p.gallery_id = :galleryId AND p.deleted_at IS NULL ORDER BY p.created_at DESC";
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("galleryId", galleryId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
        return rows.stream().map(this::toQueryItem).toList();
    }

    @Override
    public long countPosts(Long galleryId) {
        Object count = entityManager
                .createNativeQuery("SELECT COUNT(*) FROM posts WHERE gallery_id = :galleryId AND deleted_at IS NULL")
                .setParameter("galleryId", galleryId)
                .getSingleResult();
        return ((Number) count).longValue();
    }

    private PostQueryItem toQueryItem(Object[] row) {
        return new PostQueryItem(
                longValue(row[0]),
                longValue(row[1]),
                longValue(row[2]),
                stringValue(row[3]),
                nullableString(row[4]),
                stringValue(row[5]),
                stringValue(row[6]),
                intValue(row[7]),
                intValue(row[8]),
                intValue(row[9]),
                intValue(row[10]),
                localDateTime(row[11]));
    }

    private static Long longValue(Object value) {
        return ((Number) value).longValue();
    }

    private static int intValue(Object value) {
        return ((Number) value).intValue();
    }

    private static String stringValue(Object value) {
        return value.toString();
    }

    private static String nullableString(Object value) {
        return value == null ? null : stringValue(value);
    }

    private static LocalDateTime localDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        return ((Timestamp) value).toLocalDateTime();
    }
}
