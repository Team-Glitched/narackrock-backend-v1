package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.application.shorts.ShortsSource;
import glitched.adlips.application.shorts.port.out.ShortsParticipantQueryItem;
import glitched.adlips.application.shorts.port.out.ShortsQueryItem;
import glitched.adlips.application.shorts.port.out.ShortsQueryPort;
import glitched.adlips.domain.shorts.ShortStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ShortsQueryPersistenceAdapter implements ShortsQueryPort {
    private final EntityManager entityManager;

    public ShortsQueryPersistenceAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<ShortsQueryItem> findByCursor(
            Long cursor,
            ShortsSource source,
            ShortStatus status,
            int limit,
            Long currentUserId
    ) {
        if (source != ShortsSource.SHORTS_FEED) {
            throw new IllegalArgumentException("지원하지 않는 숏폼 조회 경로입니다.");
        }
        String cursorCondition = cursor == null ? "" : " AND s.id < :cursor";
        String likedExpression = reactionExpression(currentUserId, "LIKE");
        String dislikedExpression = reactionExpression(currentUserId, "DISLIKE");
        String bookmarkedExpression = currentUserId == null
                ? "FALSE"
                : "EXISTS (SELECT 1 FROM shorts_bookmarks b WHERE b.user_id = :currentUserId AND b.shorts_id = s.id)";
        String sql = """
                SELECT s.id, s.project_id, s.title,
                       media.file_url, album.file_url, s.status,
                       s.view_count, s.like_count, s.dislike_count,
                       s.comment_count, s.contribution_count,
                       %s, %s, %s,
                       p.user_id, p.nickname, profile_image.file_url, s.created_at
                  FROM shorts s
                  JOIN profiles p ON p.user_id = s.user_id
                  JOIN media_files media ON media.id = s.media_file_id
             LEFT JOIN media_files album ON album.id = s.album_image_file_id
             LEFT JOIN media_files profile_image ON profile_image.id = p.profile_image_file_id
                 WHERE s.status = :status
                   AND s.deleted_at IS NULL
                """.formatted(likedExpression, dislikedExpression, bookmarkedExpression)
                + cursorCondition
                + " ORDER BY s.id DESC";

        Query query = entityManager.createNativeQuery(sql)
                .setParameter("status", status.name())
                .setMaxResults(limit);
        if (cursor != null) {
            query.setParameter("cursor", cursor);
        }
        if (currentUserId != null) {
            query.setParameter("currentUserId", currentUserId);
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(this::toShortsQueryItem).toList();
    }

    @Override
    public List<ShortsParticipantQueryItem> findParticipants(List<Long> shortIds) {
        if (shortIds.isEmpty()) {
            return List.of();
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT sp.shorts_id, sp.user_id, p.nickname, sp.role
                          FROM shorts_participants sp
                          JOIN profiles p ON p.user_id = sp.user_id
                         WHERE sp.shorts_id IN :shortIds
                         ORDER BY sp.shorts_id DESC, sp.id ASC
                        """)
                .setParameter("shortIds", shortIds)
                .getResultList();
        return rows.stream()
                .map(row -> new ShortsParticipantQueryItem(
                        longValue(row[0]), longValue(row[1]), stringValue(row[2]), stringValue(row[3])))
                .toList();
    }

    private String reactionExpression(Long currentUserId, String reactionType) {
        if (currentUserId == null) {
            return "FALSE";
        }
        return "EXISTS (SELECT 1 FROM reactions r "
                + "WHERE r.user_id = :currentUserId AND r.target_type = 'SHORT' "
                + "AND r.target_id = s.id AND r.reaction_type = '" + reactionType + "')";
    }

    private ShortsQueryItem toShortsQueryItem(Object[] row) {
        return new ShortsQueryItem(
                longValue(row[0]),
                nullableLong(row[1]),
                stringValue(row[2]),
                stringValue(row[3]),
                nullableString(row[4]),
                ShortStatus.valueOf(stringValue(row[5])),
                intValue(row[6]),
                intValue(row[7]),
                intValue(row[8]),
                longValue(row[9]),
                intValue(row[10]),
                booleanValue(row[11]),
                booleanValue(row[12]),
                booleanValue(row[13]),
                longValue(row[14]),
                stringValue(row[15]),
                nullableString(row[16]),
                localDateTime(row[17])
        );
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
