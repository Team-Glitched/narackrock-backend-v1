package glitched.adlips.adapter.out.persistence.opensource;

import glitched.adlips.application.opensource.OpenSourceSavePort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

@Repository
public class OpenSourceSavePersistenceAdapter implements OpenSourceSavePort {
    private final EntityManager entityManager;

    public OpenSourceSavePersistenceAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean lockAvailableSource(Long openSourceId) {
        // Serialize saves on the source row before checking for an existing relation.
        return !entityManager.createQuery("""
                SELECT s FROM OpenSourceEntity s
                WHERE s.id = :id AND s.isPublic = true AND s.deletedAt IS NULL
                """, OpenSourceEntity.class)
                .setParameter("id", openSourceId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList().isEmpty();
    }

    @Override
    public boolean isSaved(Long userId, Long openSourceId) {
        return entityManager.createQuery("""
                SELECT COUNT(s) FROM OpenSourceSaveEntity s
                WHERE s.userId = :userId AND s.openSourceId = :sourceId
                """, Long.class)
                .setParameter("userId", userId).setParameter("sourceId", openSourceId)
                .getSingleResult() > 0;
    }

    @Override
    public void insertSave(Long userId, Long openSourceId) {
        entityManager.persist(new OpenSourceSaveEntity(userId, openSourceId));
        entityManager.flush();
    }

    @Override
    public void deleteSave(Long userId, Long openSourceId) {
        entityManager.createQuery("""
                DELETE FROM OpenSourceSaveEntity s
                WHERE s.userId = :userId AND s.openSourceId = :sourceId
                """)
                .setParameter("userId", userId).setParameter("sourceId", openSourceId)
                .executeUpdate();
    }
}
