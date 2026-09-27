package glitched.adlips.adapter.out.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.community.port.out.PostQueryItem;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class PostDetailViewerStateQueryTest {

    @Autowired GalleryJpaRepository galleryRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    PostQueryPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PostQueryPersistenceAdapter(galleryRepository, entityManager);
        jdbcTemplate.update("""
                INSERT INTO users (id, email, role, created_at) VALUES
                    (1, 'writer@test.com', 'USER', CURRENT_TIMESTAMP),
                    (2, 'viewer@test.com', 'USER', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("INSERT INTO galleries (id, name, created_at) VALUES (10, '자유 갤러리', CURRENT_TIMESTAMP)");
        jdbcTemplate.update("""
                INSERT INTO posts (
                    id, gallery_id, user_id, title, content,
                    view_count, like_count, dislike_count, comment_count, created_at
                ) VALUES (501, 10, 1, 'title', 'content', 0, 1, 0, 0, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO reactions (user_id, target_type, target_id, reaction_type, created_at)
                VALUES (2, 'POST', 501, 'LIKE', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("INSERT INTO post_bookmarks (user_id, post_id, created_at) VALUES (2, 501, CURRENT_TIMESTAMP)");
        entityManager.clear();
    }

    @Test
    void 조회자의_반응과_북마크_상태를_함께_조회한다() {
        PostQueryItem item = adapter.findDetail(501L, 2L).orElseThrow();

        assertThat(item.liked()).isTrue();
        assertThat(item.disliked()).isFalse();
        assertThat(item.bookmarked()).isTrue();
    }

    @Test
    void 다른_사용자의_반응과_북마크는_조회자_상태에_섞이지_않는다() {
        PostQueryItem item = adapter.findDetail(501L, 1L).orElseThrow();

        assertThat(item.liked()).isFalse();
        assertThat(item.bookmarked()).isFalse();
    }

    @Test
    void 조회자가_없으면_모두_false다() {
        PostQueryItem item = adapter.findDetail(501L, null).orElseThrow();

        assertThat(item.liked()).isFalse();
        assertThat(item.bookmarked()).isFalse();
    }
}
