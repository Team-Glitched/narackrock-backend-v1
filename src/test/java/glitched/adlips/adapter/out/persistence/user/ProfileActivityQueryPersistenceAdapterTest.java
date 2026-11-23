package glitched.adlips.adapter.out.persistence.user;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.user.profile.dto.response.UserProfileGetResponse;
import glitched.adlips.application.user.profile.port.out.ProfileFeaturedShortPort.PinResult;
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
    void 공개된_작업물과_참가작은_최대_5개까지_대표곡으로_설정되고_비공개_미참가_삭제곡은_거부된다() {
        jdbcTemplate.update("""
                INSERT INTO projects (
                    id, owner_id, title, album_image_file_id, bpm,
                    time_signature_numerator, time_signature_denominator, ppq, max_duration_ms,
                    major_version, minor_version, status, is_public, created_at
                )
                VALUES
                    (10, 1, '공개 작업곡', 0, 120, 4, 4, 480, 60000, 1, 1, 'IN_PROGRESS', true, CURRENT_TIMESTAMP),
                    (11, 1, '비공개 작업곡', 0, 120, 4, 4, 480, 60000, 1, 1, 'DRAFT', false, CURRENT_TIMESTAMP),
                    (12, 2, '공개 참여곡', 0, 120, 4, 4, 480, 60000, 1, 1, 'IN_PROGRESS', true, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO shorts (
                    id, user_id, project_id, title, media_file_id, status,
                    view_count, like_count, dislike_count, comment_count, contribution_count, created_at
                )
                VALUES
                    (30, 1, NULL, '첫 곡', 101, 'COMPLETED', 0, 0, 0, 0, 0, CURRENT_TIMESTAMP),
                    (31, 1, 10, '공개 진행 곡', 101, 'IN_PROGRESS', 0, 0, 0, 0, 0, CURRENT_TIMESTAMP),
                    (32, 2, 12, '참가한 진행 곡', 101, 'IN_PROGRESS', 0, 0, 0, 0, 0, CURRENT_TIMESTAMP),
                    (33, 2, NULL, '참가한 완성 곡', 101, 'COMPLETED', 0, 0, 0, 0, 0, CURRENT_TIMESTAMP),
                    (34, 2, NULL, '다른 유저 곡', 101, 'COMPLETED', 0, 0, 0, 0, 0, CURRENT_TIMESTAMP),
                    (35, 1, 11, '비공개 작업 중', 101, 'IN_PROGRESS', 0, 0, 0, 0, 0, CURRENT_TIMESTAMP),
                    (36, 1, NULL, '프로젝트 없는 미완성', 101, 'IN_PROGRESS', 0, 0, 0, 0, 0, CURRENT_TIMESTAMP),
                    (37, 1, NULL, '삭제한 곡', 101, 'COMPLETED', 0, 0, 0, 0, 0, CURRENT_TIMESTAMP),
                    (38, 1, NULL, '다섯번째 곡', 101, 'COMPLETED', 0, 0, 0, 0, 0, CURRENT_TIMESTAMP),
                    (39, 1, NULL, '여섯번째 곡', 101, 'COMPLETED', 0, 0, 0, 0, 0, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("UPDATE shorts SET deleted_at = CURRENT_TIMESTAMP WHERE id = 37");
        jdbcTemplate.update("""
                INSERT INTO shorts_participants (id, shorts_id, user_id, role, created_at)
                VALUES
                    (40, 32, 1, 'COMPOSER', CURRENT_TIMESTAMP),
                    (41, 33, 1, 'COMPOSER', CURRENT_TIMESTAMP)
                """);

        // 비공개/미연결 IN_PROGRESS, 미참여 타인 곡, 삭제곡은 거부
        assertThat(adapter.addFeaturedShort(1L, 34L, 5)).isEqualTo(PinResult.NOT_ELIGIBLE);
        assertThat(adapter.addFeaturedShort(1L, 35L, 5)).isEqualTo(PinResult.NOT_ELIGIBLE);
        assertThat(adapter.addFeaturedShort(1L, 36L, 5)).isEqualTo(PinResult.NOT_ELIGIBLE);
        assertThat(adapter.addFeaturedShort(1L, 37L, 5)).isEqualTo(PinResult.NOT_ELIGIBLE);

        // 최대 5개까지 대표곡 설정 및 멱등 재설정 허용
        assertThat(adapter.addFeaturedShort(1L, 30L, 5)).isEqualTo(PinResult.PINNED);
        assertThat(adapter.addFeaturedShort(1L, 33L, 5)).isEqualTo(PinResult.PINNED);
        assertThat(adapter.addFeaturedShort(1L, 32L, 5)).isEqualTo(PinResult.PINNED);
        assertThat(adapter.addFeaturedShort(1L, 31L, 5)).isEqualTo(PinResult.PINNED);
        assertThat(adapter.addFeaturedShort(1L, 31L, 5)).isEqualTo(PinResult.PINNED);
        assertThat(adapter.addFeaturedShort(1L, 38L, 5)).isEqualTo(PinResult.PINNED);
        assertThat(adapter.findByUserId(1L).pinnedShorts())
                .extracting(UserProfileGetResponse.PinnedShortActivity::shortId)
                .containsExactly(30L, 33L, 32L, 31L, 38L);

        // 6번째 추가 시도는 최대 개수 초과 반환
        assertThat(adapter.addFeaturedShort(1L, 39L, 5)).isEqualTo(PinResult.LIMIT_EXCEEDED);

        // 개별 해제 후 새 대표곡 추가 허용
        adapter.removeFeaturedShort(1L, 33L);
        assertThat(adapter.addFeaturedShort(1L, 39L, 5)).isEqualTo(PinResult.PINNED);
        assertThat(adapter.findByUserId(1L).pinnedShorts())
                .extracting(UserProfileGetResponse.PinnedShortActivity::shortId)
                .containsExactly(30L, 32L, 31L, 38L, 39L);

        // 비공개 전환 시 조회에서 제외되고 슬롯도 회수됨
        jdbcTemplate.update("UPDATE projects SET is_public = false WHERE id = 10");
        assertThat(adapter.findByUserId(1L).pinnedShorts())
                .extracting(UserProfileGetResponse.PinnedShortActivity::shortId)
                .containsExactly(30L, 32L, 38L, 39L);
        assertThat(adapter.addFeaturedShort(1L, 33L, 5)).isEqualTo(PinResult.PINNED);
        assertThat(adapter.findByUserId(1L).pinnedShorts())
                .extracting(UserProfileGetResponse.PinnedShortActivity::shortId)
                .containsExactly(30L, 32L, 38L, 39L, 33L);

        adapter.clearFeaturedShorts(1L);
        assertThat(adapter.findByUserId(1L).pinnedShorts()).isEmpty();
    }

    @Test
    void 프로필_활동_목록을_조회한다() {
        jdbcTemplate.update("""
                INSERT INTO posts (
                    id, gallery_id, user_id, title, content,
                    view_count, like_count, dislike_count, comment_count, created_at
                )
                VALUES
                    (20, 10, 1, '첫 게시글', 'content', 0, 0, 0, 0, '2026-07-09T00:00:00'),
                    (21, 10, 2, '다른 유저 글', 'content', 0, 0, 0, 0, '2026-07-09T00:00:00')
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
