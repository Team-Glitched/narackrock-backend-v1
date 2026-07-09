package glitched.adlips.adapter.out.persistence.user;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.user.profile.dto.response.UserProfileGetResponse;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ProfileActivityQueryPersistenceAdapterTest {
    @Autowired
    EntityManager entityManager;

    @Autowired
    JdbcTemplate jdbcTemplate;

    ProfileActivityQueryPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ProfileActivityQueryPersistenceAdapter(entityManager);
        jdbcTemplate.update("""
                INSERT INTO users (id, email, role, created_at)
                VALUES (1, 'profile@test.com', 'USER', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO users (id, email, role, created_at)
                VALUES (2, 'other@test.com', 'USER', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO galleries (id, name, created_at)
                VALUES (10, 'free', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO media_files (
                    id, owner_id, file_url, storage_key, file_type, file_size, status, created_at
                )
                VALUES
                    (100, 1, 'https://cdn.test/album.png', 'album', 'IMAGE', 1, 'READY', CURRENT_TIMESTAMP),
                    (101, 1, 'https://cdn.test/media.mp4', 'media', 'VIDEO', 1, 'READY', CURRENT_TIMESTAMP)
                """);
    }

    @Test
    void 프로필_활동_목록을_조회한다() {
        jdbcTemplate.update("""
                INSERT INTO posts (
                    id, gallery_id, user_id, title, content, view_count, like_count, dislike_count, created_at
                )
                VALUES
                    (20, 10, 1, '첫 게시글', 'content', 0, 0, 0, '2026-07-09T00:00:00'),
                    (21, 10, 2, '다른 유저 글', 'content', 0, 0, 0, '2026-07-09T00:00:00')
                """);
        jdbcTemplate.update("""
                INSERT INTO shorts (
                    id, user_id, title, album_image_file_id, media_file_id, status,
                    view_count, like_count, dislike_count, comment_count, contribution_count, created_at
                )
                VALUES
                    (30, 2, '참여 숏폼', 100, 101, 'COMPLETED', 0, 0, 0, 0, 0, '2026-07-09T00:00:00'),
                    (31, 2, '삭제 숏폼', 100, 101, 'COMPLETED', 0, 0, 0, 0, 0, '2026-07-09T00:00:00')
                """);
        jdbcTemplate.update("""
                UPDATE shorts
                   SET deleted_at = CURRENT_TIMESTAMP
                 WHERE id = 31
                """);
        jdbcTemplate.update("""
                INSERT INTO shorts_participants (id, shorts_id, user_id, role, created_at)
                VALUES
                    (40, 30, 1, 'COMPOSER', CURRENT_TIMESTAMP),
                    (41, 31, 1, 'COMPOSER', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO profile_pinned_shorts (id, user_id, shorts_id, display_order, created_at)
                VALUES (50, 1, 30, 0, CURRENT_TIMESTAMP)
                """);

        UserProfileGetResponse.Activities result = adapter.findByUserId(1L);

        assertThat(result.postCount()).isEqualTo(1);
        assertThat(result.posts()).extracting(UserProfileGetResponse.PostActivity::postId)
                .containsExactly(20L);
        assertThat(result.participatedShorts()).extracting(UserProfileGetResponse.ShortActivity::shortId)
                .containsExactly(30L);
        assertThat(result.participatedShorts().get(0).albumImageUrl())
                .isEqualTo("https://cdn.test/album.png");
        assertThat(result.pinnedShorts()).extracting(UserProfileGetResponse.PinnedShortActivity::shortId)
                .containsExactly(30L);
        assertThat(result.pinnedShorts().get(0).mediaUrl())
                .isEqualTo("https://cdn.test/media.mp4");
    }
}
