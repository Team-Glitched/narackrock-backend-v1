package glitched.adlips.adapter.out.persistence.shorts;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ShortPlaybackPersistenceAdapterTest {

    @Autowired ShortFormJpaRepository shortFormRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    ShortPlaybackPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ShortPlaybackPersistenceAdapter(shortFormRepository);
        jdbcTemplate.update("INSERT INTO users (id, email, role, created_at) VALUES (902, 'playback@test.com', 'USER', CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO projects (id, owner_id, title, album_image_file_id, bpm, time_signature_numerator, time_signature_denominator, ppq, max_duration_ms, major_version, minor_version, status, is_public, created_at) VALUES (8, 902, '공개곡', 1, 120, 4, 4, 480, 60000, 1, 1, 'IN_PROGRESS', true, CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO projects (id, owner_id, title, album_image_file_id, bpm, time_signature_numerator, time_signature_denominator, ppq, max_duration_ms, major_version, minor_version, status, is_public, created_at) VALUES (9, 902, '비공개곡', 1, 120, 4, 4, 480, 60000, 1, 1, 'IN_PROGRESS', false, CURRENT_TIMESTAMP)");
        insertShort(201L, "COMPLETED", null);
        insertShort(202L, "IN_PROGRESS", null);
        insertShort(203L, "COMPLETED", "CURRENT_TIMESTAMP");
        insertShort(204L, "IN_PROGRESS", null);
        jdbcTemplate.update("UPDATE shorts SET project_id = 8 WHERE id = 202");
        jdbcTemplate.update("UPDATE shorts SET project_id = 9 WHERE id = 204");
    }

    @Test
    void 공개된_미완성_숏폼도_재생_가능하다() {
        assertThat(adapter.existsActiveShort(201L)).isTrue();
        assertThat(adapter.existsActiveShort(202L)).isTrue();
        assertThat(adapter.existsActiveShort(203L)).isFalse();
        assertThat(adapter.existsActiveShort(204L)).isFalse();
        assertThat(adapter.existsActiveShort(999L)).isFalse();
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
}
