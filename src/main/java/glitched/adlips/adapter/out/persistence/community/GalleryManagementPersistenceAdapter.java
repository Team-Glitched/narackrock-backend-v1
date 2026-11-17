package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.application.community.port.out.GalleryManagementPort;
import glitched.adlips.application.community.port.out.GalleryManagerItem;
import glitched.adlips.domain.community.Gallery;
import glitched.adlips.domain.community.GalleryManager;
import glitched.adlips.domain.community.GalleryRole;
import glitched.adlips.domain.user.User;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class GalleryManagementPersistenceAdapter implements GalleryManagementPort {

    private static final String SELECT_MANAGERS = """
            SELECT gm.user_id, pr.nickname, pm.file_url, gm.created_at
              FROM gallery_managers gm
              JOIN users u ON u.id = gm.user_id AND u.deleted_at IS NULL
         LEFT JOIN profiles pr ON pr.user_id = gm.user_id
         LEFT JOIN media_files pm ON pm.id = pr.profile_image_file_id
             WHERE gm.gallery_id = :galleryId AND gm.role = 'MANAGER'
          ORDER BY gm.created_at, gm.id
            """;

    private final GalleryJpaRepository galleryRepository;
    private final GalleryManagerJpaRepository managerRepository;
    private final EntityManager entityManager;

    public GalleryManagementPersistenceAdapter(
            GalleryJpaRepository galleryRepository,
            GalleryManagerJpaRepository managerRepository,
            EntityManager entityManager
    ) {
        this.galleryRepository = galleryRepository;
        this.managerRepository = managerRepository;
        this.entityManager = entityManager;
    }

    @Override
    public Long createGallery(String name, String description) {
        return galleryRepository.save(Gallery.create(name, description)).getId();
    }

    @Override
    public void addManager(Long galleryId, Long userId, GalleryRole role) {
        managerRepository.save(GalleryManager.assign(
                entityManager.getReference(Gallery.class, galleryId),
                entityManager.getReference(User.class, userId),
                role));
    }

    @Override
    public Optional<Gallery> findGalleryForUpdate(Long galleryId) {
        return galleryRepository.findByIdForUpdate(galleryId);
    }

    @Override
    public void updateGallery(Gallery gallery) {
        galleryRepository.save(gallery);
    }

    @Override
    public List<GalleryManagerItem> findManagers(Long galleryId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(SELECT_MANAGERS)
                .setParameter("galleryId", galleryId)
                .getResultList();
        return rows.stream()
                .map(row -> new GalleryManagerItem(
                        ((Number) row[0]).longValue(),
                        row[1] == null ? null : row[1].toString(),
                        row[2] == null ? null : row[2].toString(),
                        localDateTime(row[3])))
                .toList();
    }

    @Override
    public boolean removeManager(Long galleryId, Long userId) {
        return entityManager.createNativeQuery("""
                        DELETE FROM gallery_managers
                         WHERE gallery_id = :galleryId AND user_id = :userId AND role = 'MANAGER'
                        """)
                .setParameter("galleryId", galleryId)
                .setParameter("userId", userId)
                .executeUpdate() > 0;
    }

    @Override
    public long countActiveManagers(Long galleryId) {
        Object count = entityManager.createNativeQuery("""
                        SELECT COUNT(*)
                          FROM gallery_managers gm
                          JOIN users u ON u.id = gm.user_id AND u.deleted_at IS NULL
                         WHERE gm.gallery_id = :galleryId AND gm.role = 'MANAGER'
                        """)
                .setParameter("galleryId", galleryId)
                .getSingleResult();
        return ((Number) count).longValue();
    }

    private static LocalDateTime localDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        return ((Timestamp) value).toLocalDateTime();
    }
}
