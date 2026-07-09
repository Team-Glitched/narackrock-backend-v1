package glitched.adlips.adapter.out.persistence.shorts;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.domain.shorts.ShortComment;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ShortCommentPersistenceAdapterTest {

    @Autowired ShortFormJpaRepository shortFormRepository;
    @Autowired ShortCommentJpaRepository shortCommentRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    ShortCommentPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ShortCommentPersistenceAdapter(shortFormRepository, shortCommentRepository, entityManager);

        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (1, 'commenter@test.com', 'USER', CURRENT_TIMESTAMP)");
        insertShort(201L, 1L, null);
        insertShort(202L, 1L, "CURRENT_TIMESTAMP");
        insertInProgressShort(203L, 1L);
        insertComment(301L, 201L, 1L, null, null);
        insertComment(302L, 201L, 1L, null, "CURRENT_TIMESTAMP");
        insertComment(303L, 202L, 1L, null, null);
        entityManager.clear();
    }

    @Test
    void 존재하고_삭제되지_않은_숏폼은_true를_반환한다() {
        assertThat(adapter.existsActiveShort(201L)).isTrue();
    }

    @Test
    void 삭제된_숏폼은_false를_반환한다() {
        assertThat(adapter.existsActiveShort(202L)).isFalse();
    }

    @Test
    void 존재하지_않는_숏폼은_false를_반환한다() {
        assertThat(adapter.existsActiveShort(999L)).isFalse();
    }

    @Test
    void 아직_완성되지_않은_IN_PROGRESS_숏폼도_true를_반환한다() {
        assertThat(adapter.existsActiveShort(203L)).isTrue();
    }

    @Test
    void 같은_숏폼에_속하고_삭제되지_않은_부모_댓글은_true를_반환한다() {
        assertThat(adapter.existsActiveParentComment(301L, 201L)).isTrue();
    }

    @Test
    void 다른_숏폼_소속이면_false를_반환한다() {
        assertThat(adapter.existsActiveParentComment(303L, 201L)).isFalse();
    }

    @Test
    void 삭제된_부모_댓글은_false를_반환한다() {
        assertThat(adapter.existsActiveParentComment(302L, 201L)).isFalse();
    }

    @Test
    void 존재하지_않는_부모_댓글은_false를_반환한다() {
        assertThat(adapter.existsActiveParentComment(999L, 201L)).isFalse();
    }

    @Test
    void 댓글을_저장하면_생성된_id를_반환한다() {
        Long commentId = adapter.save(201L, 1L, "멜로디가 좋아요.", null);
        entityManager.flush();

        assertThat(commentId).isNotNull();
    }

    @Test
    void 대댓글을_저장하면_parentCommentId가_반영된다() {
        Long commentId = adapter.save(201L, 1L, "저도 이 부분 좋다고 생각해요.", 301L);
        entityManager.flush();
        entityManager.clear();

        ShortComment reloaded = shortCommentRepository.findById(commentId).orElseThrow();
        assertThat(reloaded.getParentCommentId()).isEqualTo(301L);
    }

    @Test
    void 숏폼_댓글_수를_증가시키면_반영된다() {
        adapter.adjustShortCommentCount(201L, 1);
        entityManager.flush();
        entityManager.clear();

        assertThat(shortFormRepository.findById(201L).orElseThrow())
                .extracting("commentCount").isEqualTo(1);
    }

    @Test
    void 부모_댓글의_reply_count를_증가시키면_반영된다() {
        adapter.adjustParentReplyCount(301L, 1);
        entityManager.flush();
        entityManager.clear();

        assertThat(shortCommentRepository.findById(301L).orElseThrow())
                .extracting("replyCount").isEqualTo(1);
    }

    @Test
    void 존재하고_삭제되지_않은_댓글을_같은_숏폼_기준으로_조회한다() {
        assertThat(adapter.findActiveComment(301L, 201L)).isPresent();
    }

    @Test
    void 삭제된_댓글은_조회되지_않는다() {
        assertThat(adapter.findActiveComment(302L, 201L)).isEmpty();
    }

    @Test
    void 다른_숏폼_소속_댓글은_조회되지_않는다() {
        assertThat(adapter.findActiveComment(303L, 201L)).isEmpty();
    }

    @Test
    void 존재하지_않는_댓글은_조회되지_않는다() {
        assertThat(adapter.findActiveComment(999L, 201L)).isEmpty();
    }

    @Test
    void updateContent로_저장하면_내용이_반영된다() {
        ShortComment comment = adapter.findActiveComment(301L, 201L).orElseThrow();
        comment.updateContent("수정된 내용");

        adapter.updateContent(comment);
        entityManager.flush();
        entityManager.clear();

        assertThat(shortCommentRepository.findById(301L).orElseThrow().getContent())
                .isEqualTo("수정된 내용");
    }

    @Test
    void delete로_저장하면_더_이상_활성_댓글로_조회되지_않는다() {
        ShortComment comment = adapter.findActiveComment(301L, 201L).orElseThrow();
        comment.delete(LocalDateTime.now());

        adapter.updateContent(comment);
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.findActiveComment(301L, 201L)).isEmpty();
    }

    private void insertShort(long id, long userId, String deletedAtExpression) {
        String deletedAt = deletedAtExpression == null ? "NULL" : deletedAtExpression;
        jdbcTemplate.update("""
                INSERT INTO shorts (
                    id, user_id, title, media_file_id, status,
                    view_count, like_count, dislike_count, comment_count, contribution_count,
                    deleted_at, created_at
                ) VALUES (?, ?, 'test', 1, 'COMPLETED', 0, 0, 0, 0, 0, %s, CURRENT_TIMESTAMP)
                """.formatted(deletedAt), id, userId);
    }

    private void insertInProgressShort(long id, long userId) {
        jdbcTemplate.update("""
                INSERT INTO shorts (
                    id, user_id, title, media_file_id, status,
                    view_count, like_count, dislike_count, comment_count, contribution_count,
                    deleted_at, created_at
                ) VALUES (?, ?, 'test', 1, 'IN_PROGRESS', 0, 0, 0, 0, 0, NULL, CURRENT_TIMESTAMP)
                """, id, userId);
    }

    private void insertComment(long id, long shortsId, long userId, Long parentCommentId, String deletedAtExpression) {
        String deletedAt = deletedAtExpression == null ? "NULL" : deletedAtExpression;
        jdbcTemplate.update("""
                INSERT INTO shorts_comments (
                    id, shorts_id, user_id, content, parent_comment_id,
                    like_count, dislike_count, reply_count, deleted_at, created_at
                ) VALUES (?, ?, ?, 'content', ?, 0, 0, 0, %s, CURRENT_TIMESTAMP)
                """.formatted(deletedAt), id, shortsId, userId, parentCommentId);
    }
}
