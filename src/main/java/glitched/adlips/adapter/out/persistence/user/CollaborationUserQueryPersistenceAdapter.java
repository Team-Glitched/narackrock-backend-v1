package glitched.adlips.adapter.out.persistence.user;

import glitched.adlips.application.user.relation.port.out.CollaborationUserQueryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class CollaborationUserQueryPersistenceAdapter implements CollaborationUserQueryPort {
    private final EntityManager entityManager;

    public CollaborationUserQueryPersistenceAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Long> findCollaboratorIds(Long userId) {
        Query query = entityManager.createNativeQuery("""
                SELECT DISTINCT other.user_id
                  FROM shorts_participants mine
                  JOIN shorts_participants other ON other.shorts_id = mine.shorts_id
                  JOIN shorts s ON s.id = mine.shorts_id
                 WHERE mine.user_id = :userId
                   AND other.user_id <> :userId
                   AND s.deleted_at IS NULL
                 ORDER BY other.user_id ASC
                """)
                .setParameter("userId", userId);
        @SuppressWarnings("unchecked")
        List<Object> rows = query.getResultList();
        return rows.stream()
                .map(value -> ((Number) value).longValue())
                .toList();
    }
}
