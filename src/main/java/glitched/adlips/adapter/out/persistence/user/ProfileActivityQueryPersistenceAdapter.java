package glitched.adlips.adapter.out.persistence.user;

import glitched.adlips.application.user.profile.dto.response.UserProfileGetResponse;
import glitched.adlips.application.user.profile.port.out.ProfileActivityQueryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ProfileActivityQueryPersistenceAdapter implements ProfileActivityQueryPort {
    private static final int PROFILE_ACTIVITY_LIMIT = 6;

    private final EntityManager entityManager;

    public ProfileActivityQueryPersistenceAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public UserProfileGetResponse.Activities findByUserId(Long userId) {
        return new UserProfileGetResponse.Activities(
                postCount(userId),
                posts(userId),
                participatedShorts(userId),
                pinnedShorts(userId)
        );
    }

    private int postCount(Long userId) {
        Query query = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                  FROM posts p
                 WHERE p.user_id = :userId
                """)
                .setParameter("userId", userId);
        return ((Number) query.getSingleResult()).intValue();
    }

    private List<UserProfileGetResponse.PostActivity> posts(Long userId) {
        Query query = entityManager.createNativeQuery("""
                SELECT p.id, p.title
                  FROM posts p
                 WHERE p.user_id = :userId
                 ORDER BY p.created_at DESC, p.id DESC
                """)
                .setParameter("userId", userId)
                .setMaxResults(PROFILE_ACTIVITY_LIMIT);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(row -> new UserProfileGetResponse.PostActivity(
                        longValue(row[0]),
                        stringValue(row[1]),
                        null
                ))
                .toList();
    }

    private List<UserProfileGetResponse.ShortActivity> participatedShorts(Long userId) {
        Query query = entityManager.createNativeQuery("""
                SELECT s.id, s.title, album.file_url
                  FROM shorts_participants sp
                  JOIN shorts s ON s.id = sp.shorts_id
             LEFT JOIN media_files album ON album.id = s.album_image_file_id
                 WHERE sp.user_id = :userId
                   AND s.deleted_at IS NULL
                 ORDER BY s.created_at DESC, s.id DESC
                """)
                .setParameter("userId", userId)
                .setMaxResults(PROFILE_ACTIVITY_LIMIT);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(row -> new UserProfileGetResponse.ShortActivity(
                        longValue(row[0]),
                        stringValue(row[1]),
                        stringValue(row[2])
                ))
                .toList();
    }

    private List<UserProfileGetResponse.PinnedShortActivity> pinnedShorts(Long userId) {
        Query query = entityManager.createNativeQuery("""
                SELECT s.id, s.title, album.file_url, media.file_url, ps.display_order
                  FROM profile_pinned_shorts ps
                  JOIN shorts s ON s.id = ps.shorts_id
             LEFT JOIN media_files album ON album.id = s.album_image_file_id
                  JOIN media_files media ON media.id = s.media_file_id
                 WHERE ps.user_id = :userId
                   AND s.deleted_at IS NULL
                 ORDER BY ps.display_order ASC, ps.id ASC
                """)
                .setParameter("userId", userId);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(row -> new UserProfileGetResponse.PinnedShortActivity(
                        longValue(row[0]),
                        stringValue(row[1]),
                        stringValue(row[2]),
                        stringValue(row[3]),
                        intValue(row[4])
                ))
                .toList();
    }

    private static Long longValue(Object value) {
        return ((Number) value).longValue();
    }

    private static int intValue(Object value) {
        return ((Number) value).intValue();
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
