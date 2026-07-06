package glitched.adlips.adapter.out.persistence.shorts;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.shorts.port.out.ShortsCompositionQueryItem;
import glitched.adlips.domain.project.ProjectStatus;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ShortsCompositionPersistenceAdapterTest {

    @Autowired ShortFormJpaRepository shortFormRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    ShortsCompositionPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ShortsCompositionPersistenceAdapter(shortFormRepository);
        jdbcTemplate.update("INSERT INTO users (id, email, role, created_at) VALUES (902, 'composition@test.com', 'USER', CURRENT_TIMESTAMP)");
        insertProject(8L, "밤하늘 위 멜로디", "IN_PROGRESS", null);
        insertProject(9L, "삭제된 곡", "IN_PROGRESS", "CURRENT_TIMESTAMP");
        insertShort(201L, 8L, "COMPLETED");
        insertShort(202L, null, "COMPLETED");
        insertShort(203L, 9L, "COMPLETED");
        insertShort(204L, 8L, "IN_PROGRESS");
    }

    @Test
    void 프로젝트가_연결된_숏폼은_프로젝트_정보를_포함해_조회된다() {
        Optional<ShortsCompositionQueryItem> result = adapter.findByShortId(201L);

        assertThat(result).isPresent();
        ShortsCompositionQueryItem item = result.get();
        assertThat(item.shortId()).isEqualTo(201L);
        assertThat(item.projectId()).isEqualTo(8L);
        assertThat(item.projectTitle()).isEqualTo("밤하늘 위 멜로디");
        assertThat(item.projectStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(item.projectDeletedAt()).isNull();
    }

    @Test
    void 프로젝트가_연결되지_않은_숏폼은_프로젝트_필드가_null인_채로_조회된다() {
        Optional<ShortsCompositionQueryItem> result = adapter.findByShortId(202L);

        assertThat(result).isPresent();
        ShortsCompositionQueryItem item = result.get();
        assertThat(item.shortId()).isEqualTo(202L);
        assertThat(item.projectId()).isNull();
        assertThat(item.projectTitle()).isNull();
        assertThat(item.projectStatus()).isNull();
    }

    @Test
    void 연결된_프로젝트가_삭제되었으면_삭제_시각이_채워진_채로_조회된다() {
        Optional<ShortsCompositionQueryItem> result = adapter.findByShortId(203L);

        assertThat(result).isPresent();
        assertThat(result.get().projectDeletedAt()).isNotNull();
    }

    @Test
    void 존재하지_않는_숏폼은_빈_값을_반환한다() {
        assertThat(adapter.findByShortId(999L)).isEmpty();
    }

    @Test
    void 아직_완료되지_않은_숏폼은_조회하지_않는다() {
        assertThat(adapter.findByShortId(204L)).isEmpty();
    }

    private void insertProject(long id, String title, String status, String deletedAtExpression) {
        String deletedAt = deletedAtExpression == null ? "NULL" : deletedAtExpression;
        jdbcTemplate.update("""
                INSERT INTO projects (
                    id, owner_id, title, album_image_file_id, bpm,
                    time_signature_numerator, time_signature_denominator, ppq, max_duration_ms,
                    major_version, minor_version, status, is_public, deleted_at, created_at
                ) VALUES (?, 902, ?, 999, 120, 4, 4, 480, 60000, 1, 1, ?, false, %s, CURRENT_TIMESTAMP)
                """.formatted(deletedAt), id, title, status);
        entityManager.clear();
    }

    private void insertShort(long id, Long projectId, String status) {
        jdbcTemplate.update("""
                INSERT INTO shorts (
                    id, user_id, project_id, title, media_file_id, status,
                    view_count, like_count, dislike_count, comment_count, contribution_count,
                    created_at
                ) VALUES (?, 902, ?, 'test', 1, ?, 0, 0, 0, 0, 0, CURRENT_TIMESTAMP)
                """, id, projectId, status);
        entityManager.clear();
    }
}
