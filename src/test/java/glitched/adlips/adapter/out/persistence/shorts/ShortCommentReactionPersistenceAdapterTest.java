package glitched.adlips.adapter.out.persistence.shorts;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.domain.reaction.ReactionType;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ShortCommentReactionPersistenceAdapterTest {

    @Autowired ReactionJpaRepository reactionRepository;
    @Autowired ShortCommentJpaRepository shortCommentRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    ShortCommentReactionPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ShortCommentReactionPersistenceAdapter(reactionRepository, shortCommentRepository);

        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (1, 'writer@test.com', 'USER', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (2, 'viewer@test.com', 'USER', CURRENT_TIMESTAMP)");
        insertShort(201L, 1L, null);
        insertShort(202L, 1L, "CURRENT_TIMESTAMP");
        insertComment(301L, 201L, 1L, null);
        insertComment(302L, 201L, 1L, "CURRENT_TIMESTAMP");
        insertComment(303L, 202L, 1L, null);
    }

    @Test
    void 존재하고_삭제되지_않은_댓글은_같은_숏폼_기준으로_락_조회에_성공한다() {
        assertThat(adapter.lockActiveComment(301L, 201L)).isTrue();
    }

    @Test
    void 삭제된_댓글은_락_조회에_실패한다() {
        assertThat(adapter.lockActiveComment(302L, 201L)).isFalse();
    }

    @Test
    void 다른_숏폼_소속_댓글은_락_조회에_실패한다() {
        assertThat(adapter.lockActiveComment(303L, 201L)).isFalse();
    }

    @Test
    void 존재하지_않는_댓글은_락_조회에_실패한다() {
        assertThat(adapter.lockActiveComment(999L, 201L)).isFalse();
    }

    @Test
    void 리액션이_없으면_빈_값을_반환한다() {
        assertThat(adapter.findReaction(2L, 301L)).isEmpty();
    }

    @Test
    void 리액션을_저장하면_조회된다() {
        adapter.saveReaction(2L, 301L, ReactionType.LIKE);

        assertThat(adapter.findReaction(2L, 301L)).contains(ReactionType.LIKE);
    }

    @Test
    void 이미_리액션이_있으면_타입만_변경된다() {
        adapter.saveReaction(2L, 301L, ReactionType.DISLIKE);
        adapter.saveReaction(2L, 301L, ReactionType.LIKE);

        assertThat(adapter.findReaction(2L, 301L)).contains(ReactionType.LIKE);
        assertThat(reactionRepository.findAll()).hasSize(1);
    }

    @Test
    void 리액션을_삭제하면_더_이상_조회되지_않는다() {
        adapter.saveReaction(2L, 301L, ReactionType.LIKE);

        adapter.deleteReaction(2L, 301L);

        assertThat(adapter.findReaction(2L, 301L)).isEmpty();
    }

    @Test
    void 좋아요_수를_증가시키면_반영된다() {
        adapter.adjustLikeCount(301L, 1);

        assertThat(adapter.getLikeCount(301L)).isEqualTo(1);
    }

    @Test
    void 싫어요_수를_증가시키면_반영된다() {
        adapter.adjustDislikeCount(301L, 1);

        assertThat(adapter.getDislikeCount(301L)).isEqualTo(1);
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

    private void insertComment(long id, long shortsId, long userId, String deletedAtExpression) {
        String deletedAt = deletedAtExpression == null ? "NULL" : deletedAtExpression;
        jdbcTemplate.update("""
                INSERT INTO shorts_comments (
                    id, shorts_id, user_id, content, parent_comment_id,
                    like_count, dislike_count, reply_count, deleted_at, created_at
                ) VALUES (?, ?, ?, 'content', NULL, 0, 0, 0, %s, CURRENT_TIMESTAMP)
                """.formatted(deletedAt), id, shortsId, userId);
    }
}
