package glitched.adlips.adapter.out.persistence.report;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.domain.report.ReportTargetType;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ReportContentModerationPersistenceAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 12, 0);

    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    ReportContentModerationPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ReportContentModerationPersistenceAdapter(entityManager);
        for (long id : new long[] {1, 2, 3}) {
            jdbcTemplate.update("INSERT INTO users (id, email, role, created_at) VALUES (?, ?, 'USER', CURRENT_TIMESTAMP)",
                    id, "u" + id + "@test.com");
        }
        jdbcTemplate.update(
                "INSERT INTO galleries (id, name, description, created_at) VALUES (10, '재즈', '설명', CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO posts (id, gallery_id, user_id, title, content, view_count, like_count, "
                + "dislike_count, comment_count, created_at) VALUES (50, 10, 2, '제목', '내용', 0, 0, 0, 3, CURRENT_TIMESTAMP)");
        insertPostComment(70, 1, null, 2);
        insertPostComment(71, 3, 70L, 0);
        insertPostComment(72, 3, 70L, 0);
        jdbcTemplate.update("""
                INSERT INTO media_files (id, owner_id, file_url, file_type, status, created_at)
                VALUES (900, 2, 'https://cdn.test/short.mp3', 'AUDIO', 'READY', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO shorts (
                    id, user_id, title, media_file_id, status,
                    view_count, like_count, dislike_count, comment_count, contribution_count,
                    deleted_at, created_at
                ) VALUES (12, 2, 'short', 900, 'COMPLETED', 0, 0, 0, 2, 0, NULL, CURRENT_TIMESTAMP)
                """);
        insertShortComment(80, 1, null, 1);
        insertShortComment(81, 3, 80L, 0);
        entityManager.clear();
    }

    @Test
    void 콘텐츠_유형별_작성자를_찾는다() {
        assertThat(adapter.findOwnerId(ReportTargetType.POST, 50L)).contains(2L);
        assertThat(adapter.findOwnerId(ReportTargetType.POST_COMMENT, 71L)).contains(3L);
        assertThat(adapter.findOwnerId(ReportTargetType.SHORT, 12L)).contains(2L);
        assertThat(adapter.findOwnerId(ReportTargetType.SHORT_COMMENT, 80L)).contains(1L);
        assertThat(adapter.findOwnerId(ReportTargetType.POST, 999L)).isEmpty();
    }

    @Test
    void 게시글을_소프트_삭제한다() {
        adapter.deleteContent(ReportTargetType.POST, 50L, NOW);
        entityManager.clear();

        assertThat(deletedAt("posts", 50)).isEqualTo(NOW);
    }

    @Test
    void 최상위_게시글_댓글을_지우면_답글도_지우고_댓글_수를_줄인다() {
        adapter.deleteContent(ReportTargetType.POST_COMMENT, 70L, NOW);
        entityManager.clear();

        assertThat(deletedAt("post_comments", 70)).isEqualTo(NOW);
        assertThat(deletedAt("post_comments", 71)).isEqualTo(NOW);
        assertThat(deletedAt("post_comments", 72)).isEqualTo(NOW);
        assertThat(intColumn("SELECT comment_count FROM posts WHERE id = 50")).isZero();
    }

    @Test
    void 게시글_답글을_지우면_부모의_답글_수와_게시글_댓글_수를_하나씩_줄인다() {
        adapter.deleteContent(ReportTargetType.POST_COMMENT, 71L, NOW);
        entityManager.clear();

        assertThat(deletedAt("post_comments", 71)).isEqualTo(NOW);
        assertThat(deletedAt("post_comments", 72)).isNull();
        assertThat(intColumn("SELECT reply_count FROM post_comments WHERE id = 70")).isEqualTo(1);
        assertThat(intColumn("SELECT comment_count FROM posts WHERE id = 50")).isEqualTo(2);
    }

    @Test
    void 숏폼_댓글을_지우면_숏폼_댓글_수와_부모_답글_수를_줄인다() {
        adapter.deleteContent(ReportTargetType.SHORT_COMMENT, 81L, NOW);
        entityManager.clear();

        assertThat(deletedAt("shorts_comments", 81)).isEqualTo(NOW);
        assertThat(intColumn("SELECT reply_count FROM shorts_comments WHERE id = 80")).isZero();
        assertThat(intColumn("SELECT comment_count FROM shorts WHERE id = 12")).isEqualTo(1);
    }

    @Test
    void 이미_삭제된_콘텐츠는_다시_건드리지_않는다() {
        jdbcTemplate.update("UPDATE post_comments SET deleted_at = ? WHERE id = 71", NOW.minusDays(1));
        entityManager.clear();

        adapter.deleteContent(ReportTargetType.POST_COMMENT, 71L, NOW);
        entityManager.clear();

        assertThat(deletedAt("post_comments", 71)).isEqualTo(NOW.minusDays(1));
        assertThat(intColumn("SELECT reply_count FROM post_comments WHERE id = 70")).isEqualTo(2);
        assertThat(intColumn("SELECT comment_count FROM posts WHERE id = 50")).isEqualTo(3);
    }

    private void insertPostComment(long id, long userId, Long parentId, int replyCount) {
        jdbcTemplate.update("INSERT INTO post_comments (id, post_id, user_id, content, parent_comment_id, reply_count, "
                + "like_count, dislike_count, created_at) VALUES (?, 50, ?, '댓글', ?, ?, 0, 0, CURRENT_TIMESTAMP)",
                id, userId, parentId, replyCount);
    }

    private void insertShortComment(long id, long userId, Long parentId, int replyCount) {
        jdbcTemplate.update("""
                INSERT INTO shorts_comments (
                    id, shorts_id, user_id, content, parent_comment_id,
                    like_count, dislike_count, reply_count, deleted_at, created_at
                ) VALUES (?, 12, ?, 'content', ?, 0, 0, ?, NULL, CURRENT_TIMESTAMP)
                """, id, userId, parentId, replyCount);
    }

    private LocalDateTime deletedAt(String table, long id) {
        return jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM " + table + " WHERE id = ?", LocalDateTime.class, id);
    }

    private int intColumn(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
