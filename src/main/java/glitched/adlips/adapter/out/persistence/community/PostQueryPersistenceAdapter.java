package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.application.community.PostSort;
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
            SELECT p.id, p.gallery_id, g.name, p.user_id, pr.nickname, media.file_url,
                   p.title, p.content, p.view_count, p.like_count, p.dislike_count, p.comment_count,
                   FALSE, FALSE, p.created_at, p.updated_at
              FROM posts p
              JOIN galleries g ON g.id = p.gallery_id
         LEFT JOIN profiles pr ON pr.user_id = p.user_id
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
    public Optional<PostQueryItem> findDetail(Long postId, Long viewerId) {
        String sql = SELECT_POST.replace(
                        "FALSE, FALSE", reactionExpression(viewerId, "LIKE") + ", " + reactionExpression(viewerId, "DISLIKE"))
                + " WHERE p.id = :postId AND p.deleted_at IS NULL";
        Query query = entityManager.createNativeQuery(sql).setParameter("postId", postId);
        if (viewerId != null) {
            query.setParameter("viewerId", viewerId);
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().findFirst().map(this::toQueryItem);
    }

    @Override
    public List<PostQueryItem> findPosts(Long galleryId, String keyword, PostSort sort, int page, int size) {
        String keywordCondition = keyword == null
                ? ""
                : " AND (p.title LIKE :keyword OR p.content LIKE :keyword)";
        String sql = SELECT_POST
                + " WHERE p.gallery_id = :galleryId AND p.deleted_at IS NULL"
                + keywordCondition
                + " ORDER BY " + orderBy(sort);
        Query query = entityManager.createNativeQuery(sql)
                .setParameter("galleryId", galleryId)
                .setFirstResult(page * size)
                .setMaxResults(size);
        if (keyword != null) {
            query.setParameter("keyword", "%" + keyword + "%");
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(this::toQueryItem).toList();
    }

    @Override
    public long countPosts(Long galleryId, String keyword) {
        String keywordCondition = keyword == null
                ? ""
                : " AND (title LIKE :keyword OR content LIKE :keyword)";
        Query query = entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM posts WHERE gallery_id = :galleryId AND deleted_at IS NULL"
                                + keywordCondition)
                .setParameter("galleryId", galleryId);
        if (keyword != null) {
            query.setParameter("keyword", "%" + keyword + "%");
        }
        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public Optional<String> findGalleryName(Long galleryId) {
        return galleryRepository.findById(galleryId).map(glitched.adlips.domain.community.Gallery::getName);
    }

    // Kept for callers that still need to constrain a lookup by gallery while migrating to the public post URL.
    public Optional<PostQueryItem> findDetail(Long postId, Long galleryId, boolean legacyGalleryFilter) {
        Query query = entityManager.createNativeQuery(
                        SELECT_POST + " WHERE p.id = :postId AND p.gallery_id = :galleryId AND p.deleted_at IS NULL")
                .setParameter("postId", postId)
                .setParameter("galleryId", galleryId);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().findFirst().map(this::toQueryItem);
    }

    public List<PostQueryItem> findPosts(Long galleryId, int page, int size) {
        return findPosts(galleryId, null, PostSort.LATEST, page, size);
    }

    public long countPosts(Long galleryId) {
        return countPosts(galleryId, null);
    }

    private static String orderBy(PostSort sort) {
        return switch (sort) {
            case POPULAR -> "p.like_count DESC, p.comment_count DESC, p.created_at DESC, p.id DESC";
            case OLDEST -> "p.created_at ASC, p.id ASC";
            case LATEST -> "p.created_at DESC, p.id DESC";
        };
    }

    private static String reactionExpression(Long viewerId, String reactionType) {
        if (viewerId == null) {
            return "FALSE";
        }
        return "EXISTS (SELECT 1 FROM reactions r WHERE r.user_id = :viewerId"
                + " AND r.target_type = 'POST' AND r.target_id = p.id"
                + " AND r.reaction_type = '" + reactionType + "')";
    }

    private PostQueryItem toQueryItem(Object[] row) {
        return new PostQueryItem(
                longValue(row[0]),
                longValue(row[1]),
                nullableString(row[2]),
                longValue(row[3]),
                nullableString(row[4]),
                nullableString(row[5]),
                stringValue(row[6]),
                stringValue(row[7]),
                intValue(row[8]),
                intValue(row[9]),
                intValue(row[10]),
                intValue(row[11]),
                booleanValue(row[12]),
                booleanValue(row[13]),
                localDateTime(row[14]),
                nullableLocalDateTime(row[15]));
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

    private static LocalDateTime nullableLocalDateTime(Object value) {
        return value == null ? null : localDateTime(value);
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean bool ? bool : ((Number) value).intValue() != 0;
    }
}
