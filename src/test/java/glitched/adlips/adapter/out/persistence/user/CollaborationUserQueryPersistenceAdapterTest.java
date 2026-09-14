package glitched.adlips.adapter.out.persistence.user;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class CollaborationUserQueryPersistenceAdapterTest {
    @Autowired
    EntityManager entityManager;

    @Autowired
    JdbcTemplate jdbcTemplate;

    CollaborationUserQueryPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CollaborationUserQueryPersistenceAdapter(entityManager);
        jdbcTemplate.update("""
                INSERT INTO users (id, email, role, created_at)
                VALUES
                    (1, 'me@test.com', 'USER', CURRENT_TIMESTAMP),
                    (2, 'collab@test.com', 'USER', CURRENT_TIMESTAMP),
                    (3, 'deleted@test.com', 'USER', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO shorts (
                    id, user_id, title, media_file_id, status,
                    view_count, like_count, dislike_count, comment_count, contribution_count, created_at
                )
                VALUES
                    (10, 1, 'active', 100, 'COMPLETED', 0, 0, 0, 0, 0, CURRENT_TIMESTAMP),
                    (11, 1, 'deleted', 100, 'COMPLETED', 0, 0, 0, 0, 0, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("UPDATE shorts SET deleted_at = CURRENT_TIMESTAMP WHERE id = 11");
        jdbcTemplate.update("""
                INSERT INTO shorts_participants (id, shorts_id, user_id, role, created_at)
                VALUES
                    (20, 10, 1, 'OWNER', CURRENT_TIMESTAMP),
                    (21, 10, 2, 'COMPOSER', CURRENT_TIMESTAMP),
                    (22, 11, 1, 'OWNER', CURRENT_TIMESTAMP),
                    (23, 11, 3, 'COMPOSER', CURRENT_TIMESTAMP)
                """);
    }

    @Test
    void 같은_숏폼에_참여한_다른_유저만_조회한다() {
        assertThat(adapter.findCollaboratorIds(1L)).containsExactly(2L);
    }
}
