package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.application.shorts.port.out.ShortCommentQueryItem;
import glitched.adlips.application.shorts.port.out.ShortCommentQueryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ShortCommentQueryPersistenceAdapter implements ShortCommentQueryPort {

    private final ShortFormJpaRepository shortFormRepository;
    private final EntityManager entityManager;

    public ShortCommentQueryPersistenceAdapter(
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
    public List<ShortCommentQueryItem> findComments(Long shortId, Long viewerId) {
        String sql = """
                SELECT c.id, c.parent_comment_id, c.content,
                       p.user_id, p.nickname, profile_image.file_url,
                       c.like_count, c.reply_count, %s, c.created_at
                  FROM shorts_comments c
                  JOIN profiles p ON p.user_id = c.user_id
             LEFT JOIN media_files profile_image ON profile_image.id = p.profile_image_file_id
                 WHERE c.shorts_id = :shortId
                   AND c.deleted_at IS NULL
                 ORDER BY c.like_count DESC, c.created_at ASC
                """.formatted(likedExpression(viewerId));

        Query query = entityManager.createNativeQuery(sql).setParameter("shortId", shortId);
        if (viewerId != null) {
            query.setParameter("viewerId", viewerId);
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(this::toQueryItem).toList();
    }

    private String likedExpression(Long viewerId) {
        if (viewerId == null) {
            return "FALSE";
        }
        return "EXISTS (SELECT 1 FROM reactions r "
                + "WHERE r.user_id = :viewerId AND r.target_type = 'SHORT_COMMENT' "
                + "AND r.target_id = c.id AND r.reaction_type = 'LIKE')";
    }

    private ShortCommentQueryItem toQueryItem(Object[] row) {
        return new ShortCommentQueryItem(
                longValue(row[0]),
                nullableLong(row[1]),
                stringValue(row[2]),
                longValue(row[3]),
                stringValue(row[4]),
                nullableString(row[5]),
                intValue(row[6]),
                intValue(row[7]),
                booleanValue(row[8]),
                localDateTime(row[9]));
    }

    private static Long longValue(Object value) {
        return ((Number) value).longValue();
    }

    private static Long nullableLong(Object value) {
        return value == null ? null : longValue(value);
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

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean bool ? bool : ((Number) value).intValue() != 0;
    }

    private static LocalDateTime localDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        return ((Timestamp) value).toLocalDateTime();
    }
}
