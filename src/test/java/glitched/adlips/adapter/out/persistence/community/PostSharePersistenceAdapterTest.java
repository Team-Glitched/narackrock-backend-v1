package glitched.adlips.adapter.out.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class PostSharePersistenceAdapterTest {

    @Autowired PostJpaRepository postRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    PostSharePersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PostSharePersistenceAdapter(postRepository);
        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (1, 'writer@test.com', 'USER', CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO galleries (id, name, created_at) VALUES (10, '자유 갤러리', CURRENT_TIMESTAMP)");
        jdbcTemplate.update("""
                INSERT INTO posts (
                    id, gallery_id, user_id, title, content,
                    view_count, like_count, dislike_count, comment_count, deleted_at, created_at
                ) VALUES (501, 10, 1, 'title', 'content', 0, 0, 0, 0, NULL, CURRENT_TIMESTAMP),
                         (502, 10, 1, 'title', 'content', 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        entityManager.clear();
    }

    @Test
    void 삭제되지_않은_게시글만_공유할_수_있다() {
        assertThat(adapter.existsActivePost(501L)).isTrue();
        assertThat(adapter.existsActivePost(502L)).isFalse();
        assertThat(adapter.existsActivePost(999L)).isFalse();
    }
}
