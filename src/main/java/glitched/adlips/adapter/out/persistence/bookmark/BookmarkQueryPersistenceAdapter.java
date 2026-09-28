package glitched.adlips.adapter.out.persistence.bookmark;

import glitched.adlips.application.bookmark.port.out.BookmarkQueryPort;
import glitched.adlips.application.bookmark.port.out.BookmarkedPostItem;
import glitched.adlips.application.bookmark.port.out.BookmarkedShortItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class BookmarkQueryPersistenceAdapter implements BookmarkQueryPort {

    private final EntityManager entityManager;

    public BookmarkQueryPersistenceAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<BookmarkedPostItem> findBookmarkedPosts(Long userId, int page, int size) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT p.id, p.gallery_id, g.name, p.user_id, pr.nickname, profile_media.file_url,
                               p.title, p.content, post_media.file_url,
                               p.view_count, p.like_count, p.comment_count, p.created_at, b.created_at
                          FROM post_bookmarks b
                          JOIN posts p ON p.id = b.post_id
                          JOIN galleries g ON g.id = p.gallery_id
                     LEFT JOIN profiles pr ON pr.user_id = p.user_id
                     LEFT JOIN media_files profile_media ON profile_media.id = pr.profile_image_file_id
                     LEFT JOIN media_files post_media ON post_media.id = p.image_file_id
                         WHERE b.user_id = :userId AND p.deleted_at IS NULL
                         ORDER BY b.created_at DESC, b.id DESC
                        """)
                .setParameter("userId", userId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
        return rows.stream().map(row -> new BookmarkedPostItem(
                longValue(row[0]), longValue(row[1]), nullableString(row[2]), longValue(row[3]),
                nullableString(row[4]), nullableString(row[5]), nullableString(row[6]), nullableString(row[7]),
                nullableString(row[8]), intValue(row[9]), intValue(row[10]), intValue(row[11]),
                localDateTime(row[12]), localDateTime(row[13]))).toList();
    }

    @Override
    public long countBookmarkedPosts(Long userId) {
        return ((Number) entityManager.createNativeQuery("""
                        SELECT COUNT(*)
                          FROM post_bookmarks b
                          JOIN posts p ON p.id = b.post_id
                         WHERE b.user_id = :userId AND p.deleted_at IS NULL
                        """)
                .setParameter("userId", userId)
                .getSingleResult()).longValue();
    }

    // 숏폼 노출 조건은 피드(ShortsQueryPersistenceAdapter)와 같다.
    @Override
    public List<BookmarkedShortItem> findBookmarkedShorts(Long userId, Long cursor, int limit) {
        String cursorCondition = cursor == null ? "" : " AND b.id < :cursor";
        Query query = entityManager.createNativeQuery("""
                        SELECT b.id, s.id, s.title, album.file_url, s.status,
                               s.user_id, pr.nickname, profile_media.file_url,
                               s.like_count, s.comment_count, b.created_at
                          FROM shorts_bookmarks b
                          JOIN shorts s ON s.id = b.shorts_id
                     LEFT JOIN projects source_project ON source_project.id = s.project_id
                     LEFT JOIN media_files album ON album.id = s.album_image_file_id
                     LEFT JOIN profiles pr ON pr.user_id = s.user_id
                     LEFT JOIN media_files profile_media ON profile_media.id = pr.profile_image_file_id
                         WHERE b.user_id = :userId
                           AND s.deleted_at IS NULL
                           AND (s.status = 'COMPLETED'
                                OR (s.status = 'IN_PROGRESS' AND source_project.is_public = true))
                        """ + cursorCondition + " ORDER BY b.id DESC")
                .setParameter("userId", userId)
                .setMaxResults(limit);
        if (cursor != null) {
            query.setParameter("cursor", cursor);
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(row -> new BookmarkedShortItem(
                longValue(row[0]), longValue(row[1]), nullableString(row[2]), nullableString(row[3]),
                nullableString(row[4]), longValue(row[5]), nullableString(row[6]), nullableString(row[7]),
                intValue(row[8]), ((Number) row[9]).longValue(), localDateTime(row[10]))).toList();
    }

    private static Long longValue(Object value) {
        return ((Number) value).longValue();
    }

    private static int intValue(Object value) {
        return ((Number) value).intValue();
    }

    private static String nullableString(Object value) {
        return value == null ? null : value.toString();
    }

    private static LocalDateTime localDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        return ((Timestamp) value).toLocalDateTime();
    }
}
