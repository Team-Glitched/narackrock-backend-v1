package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.community.port.out.UserBanQueryPort;
import glitched.adlips.domain.community.GalleryActor;
import glitched.adlips.domain.community.GalleryRole;
import glitched.adlips.domain.user.UserRole;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class GalleryAuthorityPersistenceAdapter implements GalleryAuthorityPort {

    private static final String SELECT_ROLES = """
            SELECT u.role, gm.role
            FROM users u
            LEFT JOIN gallery_managers gm ON gm.user_id = u.id AND gm.gallery_id = :galleryId
            WHERE u.id = :userId AND u.deleted_at IS NULL
            """;

    private final EntityManager entityManager;
    private final UserBanQueryPort userBanQueryPort;

    public GalleryAuthorityPersistenceAdapter(EntityManager entityManager, UserBanQueryPort userBanQueryPort) {
        this.entityManager = entityManager;
        this.userBanQueryPort = userBanQueryPort;
    }

    @Override
    public boolean existsGallery(Long galleryId) {
        Number count = (Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM galleries WHERE id = :galleryId")
                .setParameter("galleryId", galleryId)
                .getSingleResult();
        return count.longValue() > 0;
    }

    @Override
    public Optional<GalleryActor> findActor(Long userId, Long galleryId, LocalDateTime now) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(SELECT_ROLES)
                .setParameter("userId", userId)
                .setParameter("galleryId", galleryId)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.getFirst();
        UserRole userRole = UserRole.valueOf((String) row[0]);
        GalleryRole galleryRole = row[1] == null ? null : GalleryRole.valueOf((String) row[1]);
        return Optional.of(new GalleryActor(userId, userRole, galleryRole, userBanQueryPort.isBanned(userId, now)));
    }
}
