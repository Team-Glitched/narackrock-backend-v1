package glitched.adlips.adapter.out.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.community.port.out.GalleryModerationLogItem;
import glitched.adlips.domain.community.GalleryModerationActionType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class GalleryModerationLogPersistenceAdapterTest {

    @Autowired GalleryModerationLogJpaRepository logRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    GalleryModerationLogPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new GalleryModerationLogPersistenceAdapter(logRepository, entityManager);
        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (3, 'm@test.com', 'USER', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO galleries (id, name, description, created_at) VALUES (10, '재즈', '설명', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO galleries (id, name, description, created_at) VALUES (11, '락앤롤', '설명', CURRENT_TIMESTAMP)");
        entityManager.clear();
    }

    @Test
    void 저장한_이력을_갤러리별로_최신순_커서_조회한다() {
        adapter.save(10L, 3L, GalleryModerationActionType.UPDATE_GALLERY, null, 10L, null);
        adapter.save(10L, 3L, GalleryModerationActionType.DELETE_POST, 1L, 99L, "광고");
        adapter.save(11L, 3L, GalleryModerationActionType.UPDATE_GALLERY, null, 11L, null);
        entityManager.flush();
        entityManager.clear();

        var page = adapter.findPage(10L, null, 10);

        assertThat(page).extracting(GalleryModerationLogItem::actionType).containsExactly(
                GalleryModerationActionType.DELETE_POST, GalleryModerationActionType.UPDATE_GALLERY);
        GalleryModerationLogItem latest = page.getFirst();
        assertThat(latest.actorId()).isEqualTo(3L);
        assertThat(latest.targetUserId()).isEqualTo(1L);
        assertThat(latest.targetId()).isEqualTo(99L);
        assertThat(latest.reason()).isEqualTo("광고");
        assertThat(latest.createdAt()).isNotNull();
        assertThat(adapter.findPage(10L, latest.logId(), 10)).extracting(GalleryModerationLogItem::actionType)
                .containsExactly(GalleryModerationActionType.UPDATE_GALLERY);
    }
}
