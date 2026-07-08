package glitched.adlips.adapter.out.persistence.shorts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import glitched.adlips.application.shorts.port.out.ShortCommentQueryItem;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ShortCommentQueryPersistenceAdapterTest {

    @Autowired ShortFormJpaRepository shortFormRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    ShortCommentQueryPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ShortCommentQueryPersistenceAdapter(shortFormRepository, entityManager);

        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (1, 'writer1@test.com', 'USER', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (2, 'writer2@test.com', 'USER', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (3, 'viewer@test.com', 'USER', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO profiles (user_id, nickname, is_private, follower_count, following_count) VALUES (1, 'writer_one', false, 0, 0)");
        jdbcTemplate.update(
                "INSERT INTO profiles (user_id, nickname, profile_image_file_id, is_private, follower_count, following_count) VALUES (2, 'writer_two', 100, false, 0, 0)");
        jdbcTemplate.update("""
                INSERT INTO media_files (id, owner_id, file_url, file_type, status, created_at)
                VALUES (100, 2, 'https://cdn.test/writer2.png', 'IMAGE', 'READY', CURRENT_TIMESTAMP)
                """);
        insertShort(201L, 1L, null);
        insertShort(202L, 1L, "CURRENT_TIMESTAMP");
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
    void 좋아요가_많은_순으로_정렬되고_동점이면_오래된_순이다() {
        insertComment(301L, 201L, 1L, null, 1, "CURRENT_TIMESTAMP", null);
        insertComment(302L, 201L, 1L, null, 5, "DATEADD('MINUTE', -10, CURRENT_TIMESTAMP)", null);
        insertComment(303L, 201L, 1L, null, 5, "DATEADD('MINUTE', -5, CURRENT_TIMESTAMP)", null);

        List<ShortCommentQueryItem> result = adapter.findComments(201L, null);

        assertThat(result).extracting(ShortCommentQueryItem::commentId)
                .containsExactly(302L, 303L, 301L);
    }

    @Test
    void 삭제된_댓글은_제외된다() {
        insertComment(301L, 201L, 1L, null, 0, "CURRENT_TIMESTAMP", "CURRENT_TIMESTAMP");
        insertComment(302L, 201L, 1L, null, 0, "CURRENT_TIMESTAMP", null);

        List<ShortCommentQueryItem> result = adapter.findComments(201L, null);

        assertThat(result).extracting(ShortCommentQueryItem::commentId).containsExactly(302L);
    }

    @Test
    void 다른_숏폼_소속_댓글은_포함되지_않는다() {
        insertComment(301L, 201L, 1L, null, 0, "CURRENT_TIMESTAMP", null);
        insertComment(302L, 202L, 1L, null, 0, "CURRENT_TIMESTAMP", null);

        List<ShortCommentQueryItem> result = adapter.findComments(201L, null);

        assertThat(result).extracting(ShortCommentQueryItem::commentId).containsExactly(301L);
    }

    @Test
    void 대댓글도_parentCommentId를_포함해_같은_배열에_포함된다() {
        insertComment(301L, 201L, 1L, null, 0, "CURRENT_TIMESTAMP", null);
        insertComment(302L, 201L, 1L, 301L, 0, "CURRENT_TIMESTAMP", null);

        List<ShortCommentQueryItem> result = adapter.findComments(201L, null);

        assertThat(result).extracting(ShortCommentQueryItem::commentId, ShortCommentQueryItem::parentCommentId)
                .containsExactlyInAnyOrder(tuple(301L, null), tuple(302L, 301L));
    }

    @Test
    void viewerId가_null이면_모든_항목이_isLiked_false다() {
        insertComment(301L, 201L, 1L, null, 0, "CURRENT_TIMESTAMP", null);
        jdbcTemplate.update("""
                INSERT INTO reactions (user_id, target_type, target_id, reaction_type, created_at)
                VALUES (3, 'SHORT_COMMENT', 301, 'LIKE', CURRENT_TIMESTAMP)
                """);

        List<ShortCommentQueryItem> result = adapter.findComments(201L, null);

        assertThat(result).extracting(ShortCommentQueryItem::liked).containsExactly(false);
    }

    @Test
    void viewer가_좋아요_누른_댓글만_isLiked_true다() {
        insertComment(301L, 201L, 1L, null, 0, "CURRENT_TIMESTAMP", null);
        insertComment(302L, 201L, 1L, null, 0, "CURRENT_TIMESTAMP", null);
        jdbcTemplate.update("""
                INSERT INTO reactions (user_id, target_type, target_id, reaction_type, created_at)
                VALUES (3, 'SHORT_COMMENT', 301, 'LIKE', CURRENT_TIMESTAMP)
                """);

        List<ShortCommentQueryItem> result = adapter.findComments(201L, 3L);

        assertThat(result)
                .filteredOn(item -> item.commentId().equals(301L))
                .extracting(ShortCommentQueryItem::liked)
                .containsExactly(true);
        assertThat(result)
                .filteredOn(item -> item.commentId().equals(302L))
                .extracting(ShortCommentQueryItem::liked)
                .containsExactly(false);
    }

    @Test
    void 작성자_닉네임과_프로필_이미지가_정확히_조인된다() {
        insertComment(301L, 201L, 2L, null, 0, "CURRENT_TIMESTAMP", null);

        List<ShortCommentQueryItem> result = adapter.findComments(201L, null);

        assertThat(result).extracting(
                        ShortCommentQueryItem::writerId,
                        ShortCommentQueryItem::nickname,
                        ShortCommentQueryItem::profileImageUrl)
                .containsExactly(tuple(2L, "writer_two", "https://cdn.test/writer2.png"));
    }

    @Test
    void 프로필_이미지가_없는_작성자는_profileImageUrl이_null이다() {
        insertComment(301L, 201L, 1L, null, 0, "CURRENT_TIMESTAMP", null);

        List<ShortCommentQueryItem> result = adapter.findComments(201L, null);

        assertThat(result).extracting(ShortCommentQueryItem::profileImageUrl)
                .containsExactly((String) null);
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

    private void insertComment(
            long id, long shortsId, long userId, Long parentCommentId,
            int likeCount, String createdAtExpression, String deletedAtExpression
    ) {
        String deletedAt = deletedAtExpression == null ? "NULL" : deletedAtExpression;
        jdbcTemplate.update("""
                INSERT INTO shorts_comments (
                    id, shorts_id, user_id, content, parent_comment_id,
                    like_count, dislike_count, reply_count, deleted_at, created_at
                ) VALUES (?, ?, ?, 'content', ?, ?, 0, 0, %s, %s)
                """.formatted(deletedAt, createdAtExpression), id, shortsId, userId, parentCommentId, likeCount);
    }
}
