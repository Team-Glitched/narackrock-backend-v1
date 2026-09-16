package glitched.adlips.adapter.out.persistence.view;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.view.ContentViewKey;
import glitched.adlips.application.view.ContentViewTarget;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class JpaContentViewPersistenceAdapterTest {

    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    JpaContentViewPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaContentViewPersistenceAdapter(entityManager);
        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (902, 'view@test.com', 'USER', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO galleries (id, name, created_at) VALUES (10, 'view gallery', CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO projects (id, owner_id, title, album_image_file_id, bpm, time_signature_numerator, time_signature_denominator, ppq, max_duration_ms, major_version, minor_version, status, is_public, created_at) VALUES (8, 902, '공개곡', 1, 120, 4, 4, 480, 60000, 1, 1, 'IN_PROGRESS', true, CURRENT_TIMESTAMP)");
        insertShort(12L, "COMPLETED", null);
        insertShort(13L, "IN_PROGRESS", null);
        jdbcTemplate.update("UPDATE shorts SET project_id = 8 WHERE id = 13");
        insertPost(501L, null);
        insertPost(502L, "CURRENT_TIMESTAMP");
        entityManager.clear();
    }

    @Test
    void 공개된_숏폼과_삭제되지_않은_게시글만_조회_대상이다() {
        assertThat(adapter.exists(ContentViewTarget.SHORT, 12L)).isTrue();
        assertThat(adapter.exists(ContentViewTarget.SHORT, 13L)).isTrue();
        assertThat(adapter.exists(ContentViewTarget.POST, 501L)).isTrue();
        assertThat(adapter.exists(ContentViewTarget.POST, 502L)).isFalse();
    }

    @Test
    void 숏폼과_게시글의_조회수를_원자적으로_증가시킨다() {
        adapter.increment(new ContentViewKey(ContentViewTarget.SHORT, 12L), "batch-short", 3L);
        adapter.increment(new ContentViewKey(ContentViewTarget.SHORT, 13L), "batch-public-short", 1L);
        adapter.increment(new ContentViewKey(ContentViewTarget.POST, 501L), "batch-post", 2L);
        entityManager.flush();

        assertThat(viewCount("shorts", 12L)).isEqualTo(3);
        assertThat(viewCount("shorts", 13L)).isEqualTo(1);
        assertThat(viewCount("posts", 501L)).isEqualTo(2);
    }

    @Test
    void 같은_동기화_배치는_DB에_한_번만_반영한다() {
        ContentViewKey key = new ContentViewKey(ContentViewTarget.SHORT, 12L);

        adapter.increment(key, "same-batch", 4L);
        entityManager.flush();
        entityManager.clear();
        adapter.increment(key, "same-batch", 4L);
        entityManager.flush();

        assertThat(viewCount("shorts", 12L)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM content_view_sync_batches WHERE batch_id = 'same-batch'", Integer.class))
                .isEqualTo(1);
    }

    private int viewCount(String table, long id) {
        return jdbcTemplate.queryForObject(
                "SELECT view_count FROM " + table + " WHERE id = ?", Integer.class, id);
    }

    private void insertShort(long id, String status, String deletedAtExpression) {
        String deletedAt = deletedAtExpression == null ? "NULL" : deletedAtExpression;
        jdbcTemplate.update("""
                INSERT INTO shorts (
                    id, user_id, title, media_file_id, status,
                    view_count, like_count, dislike_count, comment_count, contribution_count,
                    deleted_at, created_at
                ) VALUES (?, 902, 'test', 1, ?, 0, 0, 0, 0, 0, %s, CURRENT_TIMESTAMP)
                """.formatted(deletedAt), id, status);
    }

    private void insertPost(long id, String deletedAtExpression) {
        String deletedAt = deletedAtExpression == null ? "NULL" : deletedAtExpression;
        jdbcTemplate.update("""
                INSERT INTO posts (
                    id, gallery_id, user_id, title, content,
                    view_count, like_count, dislike_count, comment_count, deleted_at, created_at
                ) VALUES (?, 10, 902, 'title', 'content', 0, 0, 0, 0, %s, CURRENT_TIMESTAMP)
                """.formatted(deletedAt), id);
    }
}
