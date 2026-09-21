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
                SELECT DISTINCT collaboration.collaborator_id
                  FROM (
                        SELECT other.user_id AS collaborator_id
                          FROM shorts_participants mine
                          JOIN shorts_participants other
                            ON other.shorts_id = mine.shorts_id
                          JOIN shorts short_form
                            ON short_form.id = mine.shorts_id
                         WHERE mine.user_id = :userId
                           AND other.user_id <> :userId
                           AND short_form.deleted_at IS NULL

                        UNION

                        SELECT other_project_user.user_id AS collaborator_id
                          FROM (
                                SELECT project_id, user_id
                                  FROM project_members
                                UNION
                                SELECT project_id, user_id
                                  FROM project_contributions
                                 WHERE approval_status = 'APPROVED'
                               ) mine_project_user
                          JOIN (
                                SELECT project_id, user_id
                                  FROM project_members
                                UNION
                                SELECT project_id, user_id
                                  FROM project_contributions
                                 WHERE approval_status = 'APPROVED'
                               ) other_project_user
                            ON other_project_user.project_id = mine_project_user.project_id
                          JOIN projects project
                            ON project.id = mine_project_user.project_id
                         WHERE mine_project_user.user_id = :userId
                           AND other_project_user.user_id <> :userId
                           AND project.deleted_at IS NULL
                       ) collaboration
                 ORDER BY collaboration.collaborator_id ASC
                """)
                .setParameter("userId", userId);
        @SuppressWarnings("unchecked")
        List<Object> rows = query.getResultList();
        return rows.stream()
                .map(value -> ((Number) value).longValue())
                .toList();
    }
}
