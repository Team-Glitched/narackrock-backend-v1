package glitched.adlips.adapter.out.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.domain.community.Post;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class PostPersistenceAdapterTest {

    @Autowired GalleryJpaRepository galleryRepository;
    @Autowired PostJpaRepository postRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    PostPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PostPersistenceAdapter(galleryRepository, postRepository, entityManager);

        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (1, 'writer@test.com', 'USER', CURRENT_TIMESTAMP)");
        insertGallery(10L, "자유 갤러리");
        insertPost(501L, 10L, 1L, null);
        insertPost(502L, 10L, 1L, "CURRENT_TIMESTAMP");
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
    void 게시글을_저장하면_생성된_id를_반환한다() {
        Long postId = adapter.save(10L, 1L, "제목", "내용");
        entityManager.flush();

        assertThat(postId).isNotNull();
    }

    @Test
    void 존재하고_삭제되지_않은_게시글을_같은_갤러리_기준으로_조회한다() {
        assertThat(adapter.findActivePost(501L, 10L)).isPresent();
    }

    @Test
    void 삭제된_게시글은_조회되지_않는다() {
        assertThat(adapter.findActivePost(502L, 10L)).isEmpty();
    }

    @Test
    void 다른_갤러리_소속_게시글은_조회되지_않는다() {
        assertThat(adapter.findActivePost(501L, 999L)).isEmpty();
    }

    @Test
    void update로_저장하면_변경사항이_반영된다() {
        Post post = adapter.findActivePost(501L, 10L).orElseThrow();
        post.updateTitleAndContent("수정된 제목", "수정된 내용");

        adapter.update(post);
        entityManager.flush();
        entityManager.clear();

        assertThat(postRepository.findById(501L).orElseThrow().getTitle()).isEqualTo("수정된 제목");
    }

    @Test
    void delete로_저장하면_더_이상_활성_게시글로_조회되지_않는다() {
        Post post = adapter.findActivePost(501L, 10L).orElseThrow();
        post.delete(LocalDateTime.now());

        adapter.update(post);
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.findActivePost(501L, 10L)).isEmpty();
    }

    private void insertGallery(long id, String name) {
        jdbcTemplate.update(
                "INSERT INTO galleries (id, name, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)", id, name);
    }

    private void insertPost(long id, long galleryId, long userId, String deletedAtExpression) {
        String deletedAt = deletedAtExpression == null ? "NULL" : deletedAtExpression;
        jdbcTemplate.update("""
                INSERT INTO posts (
                    id, gallery_id, user_id, title, content,
                    view_count, like_count, dislike_count, comment_count, deleted_at, created_at
                ) VALUES (?, ?, ?, 'title', 'content', 0, 0, 0, 0, %s, CURRENT_TIMESTAMP)
                """.formatted(deletedAt), id, galleryId, userId);
    }
}
