package glitched.adlips.adapter.out.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import glitched.adlips.application.community.port.out.PostQueryItem;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class PostQueryPersistenceAdapterTest {

    @Autowired GalleryJpaRepository galleryRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    PostQueryPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PostQueryPersistenceAdapter(galleryRepository, entityManager);

        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (1, 'writer@test.com', 'USER', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO profiles (user_id, nickname, profile_image_file_id, is_private, follower_count, following_count) VALUES (1, 'writer_one', 100, false, 0, 0)");
        jdbcTemplate.update("""
                INSERT INTO media_files (id, owner_id, file_url, file_type, status, created_at)
                VALUES (100, 1, 'https://cdn.test/writer.png', 'IMAGE', 'READY', CURRENT_TIMESTAMP)
                """);
        insertGallery(10L, "자유 갤러리");
        insertPost(501L, 10L, 1L, "첫 번째 글", null, "2026-08-20 10:00:00");
        insertPost(502L, 10L, 1L, "두 번째 글", null, "2026-08-21 10:00:00");
        insertPost(503L, 10L, 1L, "삭제된 글", "CURRENT_TIMESTAMP", "2026-08-22 10:00:00");
        entityManager.clear();
    }

    @Test
    void 존재하는_갤러리는_true를_반환한다() {
        assertThat(adapter.existsGallery(10L)).isTrue();
    }

    @Test
    void 존재하지_않는_갤러리는_false를_반환한다() {
        assertThat(adapter.existsGallery(999L)).isFalse();
    }

    @Test
    void 상세_조회시_작성자_정보를_포함해_반환한다() {
        Optional<PostQueryItem> result = adapter.findDetail(501L, 10L);

        assertThat(result).isPresent();
        PostQueryItem item = result.orElseThrow();
        assertThat(item.postId()).isEqualTo(501L);
        assertThat(item.galleryId()).isEqualTo(10L);
        assertThat(item.userId()).isEqualTo(1L);
        assertThat(item.nickname()).isEqualTo("writer_one");
        assertThat(item.profileImageUrl()).isEqualTo("https://cdn.test/writer.png");
        assertThat(item.title()).isEqualTo("첫 번째 글");
    }

    @Test
    void 삭제된_게시글은_상세조회되지_않는다() {
        assertThat(adapter.findDetail(503L, 10L)).isEmpty();
    }

    @Test
    void 다른_갤러리_소속이면_상세조회되지_않는다() {
        assertThat(adapter.findDetail(501L, 999L)).isEmpty();
    }

    @Test
    void 목록_조회시_삭제되지_않은_글만_최신순으로_반환한다() {
        List<PostQueryItem> posts = adapter.findPosts(10L, 0, 20);

        assertThat(posts).extracting(PostQueryItem::title)
                .containsExactly("두 번째 글", "첫 번째 글");
    }

    @Test
    void 목록_조회시_페이지네이션이_적용된다() {
        List<PostQueryItem> posts = adapter.findPosts(10L, 1, 1);

        assertThat(posts).extracting(PostQueryItem::title).containsExactly("첫 번째 글");
    }

    @Test
    void 목록_전체_개수는_삭제되지_않은_글만_센다() {
        assertThat(adapter.countPosts(10L)).isEqualTo(2L);
    }

    private void insertGallery(long id, String name) {
        jdbcTemplate.update(
                "INSERT INTO galleries (id, name, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)", id, name);
    }

    private void insertPost(
            long id, long galleryId, long userId, String title, String deletedAtExpression, String createdAt) {
        String deletedAt = deletedAtExpression == null ? "NULL" : deletedAtExpression;
        jdbcTemplate.update("""
                INSERT INTO posts (
                    id, gallery_id, user_id, title, content,
                    view_count, like_count, dislike_count, comment_count, deleted_at, created_at
                ) VALUES (?, ?, ?, ?, 'content', 0, 0, 0, 0, %s, ?)
                """.formatted(deletedAt), id, galleryId, userId, title, createdAt);
    }
}
