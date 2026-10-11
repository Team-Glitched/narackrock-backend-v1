package glitched.adlips.adapter.out.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class PostBookmarkPersistenceAdapterTest {

    @Autowired PostJpaRepository postRepository;
    @Autowired PostBookmarkJpaRepository bookmarkRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    PostBookmarkPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PostBookmarkPersistenceAdapter(postRepository, bookmarkRepository);
        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (1, 'writer@test.com', 'USER', CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO galleries (id, name, created_at) VALUES (10, '자유 갤러리', CURRENT_TIMESTAMP)");
        insertPost(501L, "NULL");
        insertPost(502L, "CURRENT_TIMESTAMP");
        entityManager.clear();
    }

    @Test
    void 삭제되지_않은_게시글만_잠금_조회한다() {
        assertThat(adapter.lockActivePost(501L)).isTrue();
        assertThat(adapter.lockActivePost(502L)).isFalse();
    }

    @Test
    void 북마크를_저장하고_삭제한다() {
        adapter.saveBookmark(1L, 501L);
        assertThat(adapter.isBookmarked(1L, 501L)).isTrue();

        adapter.deleteBookmark(1L, 501L);
        assertThat(adapter.isBookmarked(1L, 501L)).isFalse();
    }

    @Test
    void 한_사용자는_같은_게시글을_한_번만_북마크할_수_있다() {
        adapter.saveBookmark(1L, 501L);

        assertThatThrownBy(() -> adapter.saveBookmark(1L, 501L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertPost(long id, String deletedAt) {
        jdbcTemplate.update("""
                INSERT INTO posts (
                    id, gallery_id, user_id, title, content,
                    view_count, like_count, dislike_count, comment_count, deleted_at, created_at
                ) VALUES (?, 10, 1, 'title', 'content', 0, 0, 0, 0, %s, CURRENT_TIMESTAMP)
                """.formatted(deletedAt), id);
    }
}
