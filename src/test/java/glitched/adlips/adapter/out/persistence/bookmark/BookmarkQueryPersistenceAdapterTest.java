package glitched.adlips.adapter.out.persistence.bookmark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import glitched.adlips.application.bookmark.port.out.BookmarkedPostItem;
import glitched.adlips.application.bookmark.port.out.BookmarkedShortItem;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class BookmarkQueryPersistenceAdapterTest {

    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    BookmarkQueryPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new BookmarkQueryPersistenceAdapter(entityManager);
        jdbcTemplate.update("""
                INSERT INTO users (id, email, role, created_at) VALUES
                    (1, 'me@test.com', 'USER', CURRENT_TIMESTAMP),
                    (2, 'writer@test.com', 'USER', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO profiles (user_id, nickname, is_private, follower_count, following_count)
                VALUES (1, 'me', false, 0, 0), (2, 'writer', false, 0, 0)
                """);
        jdbcTemplate.update("INSERT INTO galleries (id, name, created_at) VALUES (10, '음악', CURRENT_TIMESTAMP)");
        insertPost(501L, "NULL");
        insertPost(502L, "NULL");
        insertPost(503L, "CURRENT_TIMESTAMP");
        insertPost(504L, "NULL");
        insertPostBookmark(1L, 501L, "'2026-09-20 10:00:00'");
        insertPostBookmark(1L, 502L, "'2026-09-22 10:00:00'");
        insertPostBookmark(1L, 503L, "'2026-09-23 10:00:00'");
        insertPostBookmark(2L, 504L, "'2026-09-24 10:00:00'");

        jdbcTemplate.update("""
                INSERT INTO media_files (id, owner_id, file_url, file_type, status, created_at)
                VALUES (900, 2, 'https://cdn.test/short.mp3', 'AUDIO', 'READY', CURRENT_TIMESTAMP)
                """);
        insertShort(201L, "COMPLETED", "NULL");
        insertShort(202L, "COMPLETED", "NULL");
        insertShort(203L, "COMPLETED", "CURRENT_TIMESTAMP");
        insertShort(204L, "IN_PROGRESS", "NULL");
        insertShortBookmark(11L, 1L, 201L);
        insertShortBookmark(12L, 1L, 202L);
        insertShortBookmark(13L, 1L, 203L);
        insertShortBookmark(14L, 1L, 204L);
        insertShortBookmark(15L, 2L, 201L);
        entityManager.clear();
    }

    @Test
    void 내가_북마크한_삭제되지_않은_게시글을_최근_북마크순으로_조회한다() {
        List<BookmarkedPostItem> items = adapter.findBookmarkedPosts(1L, 0, 20);

        assertThat(items)
                .extracting(BookmarkedPostItem::postId, BookmarkedPostItem::galleryName, BookmarkedPostItem::nickname)
                .containsExactly(tuple(502L, "음악", "writer"), tuple(501L, "음악", "writer"));
        assertThat(items.getFirst().bookmarkedAt()).isNotNull();
        assertThat(adapter.countBookmarkedPosts(1L)).isEqualTo(2);
    }

    @Test
    void 북마크한_게시글을_페이지로_나눠_조회한다() {
        assertThat(adapter.findBookmarkedPosts(1L, 1, 1)).extracting(BookmarkedPostItem::postId).containsExactly(501L);
    }

    @Test
    void 내가_북마크한_볼_수_있는_숏폼을_최근_북마크순으로_조회한다() {
        List<BookmarkedShortItem> items = adapter.findBookmarkedShorts(1L, null, 20);

        assertThat(items)
                .extracting(BookmarkedShortItem::bookmarkId, BookmarkedShortItem::shortId, BookmarkedShortItem::nickname)
                .containsExactly(tuple(12L, 202L, "writer"), tuple(11L, 201L, "writer"));
    }

    @Test
    void 숏폼_북마크는_커서보다_작은_북마크_id부터_조회한다() {
        assertThat(adapter.findBookmarkedShorts(1L, 12L, 20))
                .extracting(BookmarkedShortItem::bookmarkId).containsExactly(11L);
        assertThat(adapter.findBookmarkedShorts(1L, null, 1))
                .extracting(BookmarkedShortItem::bookmarkId).containsExactly(12L);
    }

    private void insertPost(long id, String deletedAt) {
        jdbcTemplate.update("""
                INSERT INTO posts (
                    id, gallery_id, user_id, title, content,
                    view_count, like_count, dislike_count, comment_count, deleted_at, created_at
                ) VALUES (?, 10, 2, 'title', 'content', 0, 0, 0, 0, %s, CURRENT_TIMESTAMP)
                """.formatted(deletedAt), id);
    }

    private void insertPostBookmark(long userId, long postId, String createdAt) {
        jdbcTemplate.update(
                "INSERT INTO post_bookmarks (user_id, post_id, created_at) VALUES (?, ?, %s)".formatted(createdAt),
                userId, postId);
    }

    private void insertShort(long id, String status, String deletedAt) {
        jdbcTemplate.update("""
                INSERT INTO shorts (
                    id, user_id, title, media_file_id, status,
                    view_count, like_count, dislike_count, comment_count, contribution_count,
                    deleted_at, created_at
                ) VALUES (?, 2, 'short', 900, ?, 0, 0, 0, 0, 0, %s, CURRENT_TIMESTAMP)
                """.formatted(deletedAt), id, status);
    }

    private void insertShortBookmark(long id, long userId, long shortId) {
        jdbcTemplate.update(
                "INSERT INTO shorts_bookmarks (id, user_id, shorts_id, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                id, userId, shortId);
    }
}
