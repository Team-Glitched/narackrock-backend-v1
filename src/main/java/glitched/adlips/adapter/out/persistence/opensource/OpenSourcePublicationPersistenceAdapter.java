package glitched.adlips.adapter.out.persistence.opensource;

import glitched.adlips.application.opensource.OpenSourcePublicationPort;
import glitched.adlips.domain.opensource.OpenSourcePublication;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class OpenSourcePublicationPersistenceAdapter implements OpenSourcePublicationPort {
    private final EntityManager entityManager;

    public OpenSourcePublicationPersistenceAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<OpenSourcePublication> findOwned(Long id, Long ownerId, boolean forUpdate) {
        var query = entityManager.createQuery("""
                SELECT s FROM OpenSourceEntity s WHERE s.id = :id AND s.ownerId = :ownerId
                AND s.mediaFileId IS NOT NULL AND s.title IS NOT NULL
                AND s.createdAt IS NOT NULL AND s.updatedAt IS NOT NULL
                """, OpenSourceEntity.class).setParameter("id", id).setParameter("ownerId", ownerId);
        if (forUpdate) query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        return query.getResultStream().findFirst().map(OpenSourceEntity::toDomain);
    }

    @Override
    public OpenSourcePublication save(OpenSourcePublication source) {
        OpenSourceEntity entity;
        if (source.id() == null) {
            entity = new OpenSourceEntity();
            entity.apply(source);
            entityManager.persist(entity);
        } else {
            entity = entityManager.find(OpenSourceEntity.class, source.id());
            if (entity == null) throw new IllegalStateException("잠금 조회한 오픈소스가 없습니다.");
            entity.apply(source);
        }
        entityManager.flush();
        return entity.toDomain();
    }
}
