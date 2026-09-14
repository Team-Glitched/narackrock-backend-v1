package glitched.adlips.adapter.out.persistence.view;

import glitched.adlips.application.view.ContentViewKey;
import glitched.adlips.application.view.ContentViewTarget;
import glitched.adlips.application.view.port.out.ContentViewExistencePort;
import glitched.adlips.application.view.port.out.ContentViewPersistencePort;
import jakarta.persistence.EntityManager;
import java.util.Objects;
import org.springframework.stereotype.Repository;

@Repository
public class JpaContentViewPersistenceAdapter
        implements ContentViewExistencePort, ContentViewPersistencePort {

    private final EntityManager entityManager;

    public JpaContentViewPersistenceAdapter(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager);
    }

    @Override
    public boolean exists(ContentViewTarget target, Long contentId) {
        String query = switch (target) {
            case SHORT -> """
                    SELECT COUNT(s.id) FROM ShortForm s
                    WHERE s.id = :contentId
                      AND s.status = glitched.adlips.domain.shorts.ShortStatus.COMPLETED
                      AND s.deletedAt IS NULL
                    """;
            case POST -> """
                    SELECT COUNT(p.id) FROM Post p
                    WHERE p.id = :contentId AND p.deletedAt IS NULL
                    """;
        };
        return entityManager.createQuery(query, Long.class)
                .setParameter("contentId", contentId)
                .getSingleResult() > 0;
    }

    @Override
    public void increment(ContentViewKey key, String batchId, long count) {
        if (entityManager.find(ContentViewSyncBatchEntity.class, batchId) != null) {
            return;
        }

        int increment = Math.toIntExact(count);
        String query = switch (key.target()) {
            case SHORT -> """
                    UPDATE ShortForm s SET s.viewCount = s.viewCount + :count
                    WHERE s.id = :contentId
                      AND s.status = glitched.adlips.domain.shorts.ShortStatus.COMPLETED
                      AND s.deletedAt IS NULL
                    """;
            case POST -> """
                    UPDATE Post p SET p.viewCount = p.viewCount + :count
                    WHERE p.id = :contentId AND p.deletedAt IS NULL
                    """;
        };
        entityManager.createQuery(query)
                .setParameter("count", increment)
                .setParameter("contentId", key.contentId())
                .executeUpdate();
        entityManager.persist(new ContentViewSyncBatchEntity(batchId, key, count));
    }
}
