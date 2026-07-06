package glitched.adlips.adapter.out.persistence.project;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ProjectMemberJoinPersistenceAdapterTest {

    @Autowired ProjectJpaRepository projects;
    @Autowired ProjectMemberJpaRepository members;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    ProjectMemberJoinPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ProjectMemberJoinPersistenceAdapter(projects, members);
        insertUser(910L, "owner@join.test");
        insertUser(911L, "editor@join.test");
        insertProject(301L, null);
        insertProject(302L, "CURRENT_TIMESTAMP");
    }

    @Test
    void 삭제되지_않은_프로젝트만_잠금_조회한다() {
        assertThat(adapter.findActiveProjectForUpdate(301L)).isPresent();
        assertThat(adapter.findActiveProjectForUpdate(302L)).isEmpty();
        assertThat(adapter.findActiveProjectForUpdate(999L)).isEmpty();
    }

    @Test
    void 프로젝트_잠금_조회는_비관적_쓰기_잠금을_사용한다() throws Exception {
        Method method = ProjectJpaRepository.class
                .getDeclaredMethod("findByIdAndDeletedAtIsNullForUpdate", Long.class);

        assertThat(method.getAnnotation(Lock.class).value())
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void 사용자를_EDITOR_멤버로_저장한다() {
        Project project = projects.findById(301L).orElseThrow();
        User user = entityManager.find(User.class, 911L);

        adapter.saveEditor(project, user);
        members.flush();

        assertThat(adapter.existsByProjectIdAndUserId(301L, 911L)).isTrue();
        assertThat(members.findByProjectIdAndUserId(301L, 911L).orElseThrow().getRole())
                .isEqualTo(ProjectMemberRole.EDITOR);
    }

    private void insertUser(long id, String email) {
        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (?, ?, 'USER', CURRENT_TIMESTAMP)",
                id, email);
    }

    private void insertProject(long id, String deletedAtExpression) {
        String deletedAt = deletedAtExpression == null ? "NULL" : deletedAtExpression;
        jdbcTemplate.update("""
                INSERT INTO projects (
                    id, owner_id, title, album_image_file_id, bpm,
                    time_signature_numerator, time_signature_denominator, ppq, max_duration_ms,
                    major_version, minor_version, status, is_public, deleted_at, created_at
                ) VALUES (?, 910, 'join test', 999, 120, 4, 4, 480, 60000,
                    1, 1, 'IN_PROGRESS', false, %s, CURRENT_TIMESTAMP)
                """.formatted(deletedAt), id);
        entityManager.clear();
    }
}
