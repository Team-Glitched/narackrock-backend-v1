package glitched.adlips.adapter.out.persistence.shorts;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.shorts.port.out.ShortsComposerQueryItem;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ShortsComposersPersistenceAdapterTest {

    @Autowired ShortFormJpaRepository shortFormRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    ShortsComposersPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ShortsComposersPersistenceAdapter(shortFormRepository, entityManager);

        insertUser(3L, "guitar@test.com");
        insertUser(7L, "vocal@test.com");
        insertUser(902L, "author@test.com");
        insertMedia(501L, 3L, "https://cdn.example.com/profiles/3.png", "IMAGE");
        insertProfile(3L, "guitar_moon", 501L);
        insertProfile(7L, "vocal_wave", null);

        insertShort(201L, "IN_PROGRESS", null);
        insertShort(202L, "IN_PROGRESS", null);
        insertShort(203L, "COMPLETED", "CURRENT_TIMESTAMP");

        insertParticipant(201L, 3L, "기타", "메인 기타 리프를 만들었습니다.");
        insertParticipant(201L, 7L, "보컬", "후렴 멜로디와 보컬 라인을 추가했습니다.");
    }

    @Test
    void 참여자가_있으면_닉네임_프로필이미지_역할_설명을_포함해_순서대로_조회한다() {
        List<ShortsComposerQueryItem> result = adapter.findByShortId(201L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).userId()).isEqualTo(3L);
        assertThat(result.get(0).nickname()).isEqualTo("guitar_moon");
        assertThat(result.get(0).profileImageUrl()).isEqualTo("https://cdn.example.com/profiles/3.png");
        assertThat(result.get(0).role()).isEqualTo("기타");
        assertThat(result.get(0).description()).isEqualTo("메인 기타 리프를 만들었습니다.");
        assertThat(result.get(1).userId()).isEqualTo(7L);
    }

    @Test
    void 프로필_이미지가_없으면_profileImageUrl이_null이다() {
        List<ShortsComposerQueryItem> result = adapter.findByShortId(201L);

        ShortsComposerQueryItem vocal = result.stream()
                .filter(item -> item.userId().equals(7L))
                .findFirst()
                .orElseThrow();
        assertThat(vocal.profileImageUrl()).isNull();
    }

    @Test
    void 참여자가_없는_숏폼은_빈_리스트를_반환한다() {
        assertThat(adapter.findByShortId(202L)).isEmpty();
    }

    @Test
    void 존재하고_삭제되지_않은_숏폼은_활성_상태다() {
        assertThat(adapter.existsActiveShort(201L)).isTrue();
    }

    @Test
    void 완료_상태여도_삭제되지_않았으면_활성_상태다() {
        assertThat(adapter.existsActiveShort(203L)).isFalse();
    }

    @Test
    void 존재하지_않는_숏폼은_비활성_상태다() {
        assertThat(adapter.existsActiveShort(999L)).isFalse();
    }

    private void insertUser(long id, String email) {
        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (?, ?, 'USER', CURRENT_TIMESTAMP)",
                id, email);
    }

    private void insertProfile(long userId, String nickname, Long profileImageFileId) {
        jdbcTemplate.update("""
                INSERT INTO profiles (user_id, nickname, profile_image_file_id, is_private, follower_count, following_count)
                VALUES (?, ?, ?, false, 0, 0)
                """, userId, nickname, profileImageFileId);
    }

    private void insertMedia(long id, long ownerId, String url, String type) {
        jdbcTemplate.update("""
                INSERT INTO media_files (id, owner_id, file_url, storage_key, original_filename, file_type, mime_type, file_size, status, created_at)
                VALUES (?, ?, ?, ?, 'file', ?, 'application/octet-stream', 1, 'READY', CURRENT_TIMESTAMP)
                """, id, ownerId, url, "key-" + id, type);
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
        entityManager.clear();
    }

    private void insertParticipant(long shortId, long userId, String role, String description) {
        jdbcTemplate.update("""
                INSERT INTO shorts_participants (shorts_id, user_id, role, description, created_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, shortId, userId, role, description);
        entityManager.clear();
    }
}
