package glitched.adlips.adapter.out.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class PopularPostQueryPersistenceAdapterTest {

    @Autowired GalleryJpaRepository galleryRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    PostQueryPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PostQueryPersistenceAdapter(galleryRepository, entityManager);
        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (1, 'writer@test.com', 'USER', CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO galleries (id, name, created_at) VALUES (10, '음악', CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO galleries (id, name, created_at) VALUES (20, '기타', CURRENT_TIMESTAMP)");
        insertPost(501L, 10L, 5, 0, "'2026-09-20 10:00:00'", "NULL");
        insertPost(502L, 20L, 9, 0, "'2026-09-20 10:00:00'", "NULL");
        insertPost(503L, 10L, 5, 3, "'2026-09-20 10:00:00'", "NULL");
        insertPost(504L, 20L, 5, 3, "'2026-09-21 10:00:00'", "NULL");
        insertPost(505L, 10L, 99, 0, "'2026-09-20 10:00:00'", "CURRENT_TIMESTAMP");
        entityManager.clear();
    }

    @Test
    void 모든_갤러리의_삭제되지_않은_글의_점수를_조회한다() {
        assertThat(adapter.findPopularityScores())
                .extracting(p -> p.postId(), p -> p.viewCount(), p -> p.likeCount())
                .containsExactlyInAnyOrder(
                        tuple(501L, 0, 5), tuple(502L, 0, 9),
                        tuple(503L, 0, 5), tuple(504L, 0, 5));
    }

    @Test
    void 순위_ID로_여러_갤러리의_활성_글을_조회한다() {
        assertThat(adapter.findByIds(java.util.List.of(504L, 502L, 505L)))
                .extracting(p -> p.postId(), p -> p.galleryName())
                .containsExactlyInAnyOrder(tuple(502L, "기타"), tuple(504L, "기타"));
    }

    @Test
    void 모든_갤러리의_삭제되지_않은_글_수를_센다() {
        assertThat(adapter.countAllPosts()).isEqualTo(4);
    }

    private void insertPost(long id, long galleryId, int likeCount, int commentCount, String createdAt, String deletedAt) {
        jdbcTemplate.update("""
                INSERT INTO posts (
                    id, gallery_id, user_id, title, content,
                    view_count, like_count, dislike_count, comment_count, deleted_at, created_at
                ) VALUES (?, ?, 1, 'title', 'content', 0, ?, 0, ?, %s, %s)
                """.formatted(deletedAt, createdAt), id, galleryId, likeCount, commentCount);
    }
}
