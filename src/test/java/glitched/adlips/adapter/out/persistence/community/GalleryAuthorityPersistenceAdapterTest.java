package glitched.adlips.adapter.out.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.community.port.out.UserBanQueryPort;
import glitched.adlips.domain.community.GalleryActor;
import glitched.adlips.domain.community.GalleryRole;
import glitched.adlips.domain.user.UserRole;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class GalleryAuthorityPersistenceAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 12, 0);

    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    GalleryAuthorityPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        UserBanQueryPort banQueryPort = (userId, now) -> userId.equals(4L) && now.equals(NOW);
        adapter = new GalleryAuthorityPersistenceAdapter(entityManager, banQueryPort);

        insertUser(1, "USER", null);
        insertUser(3, "USER", null);
        insertUser(4, "USER", null);
        insertUser(9, "ADMIN", null);
        insertUser(7, "USER", "CURRENT_TIMESTAMP");
        jdbcTemplate.update(
                "INSERT INTO galleries (id, name, description, created_at) VALUES (10, '재즈', '설명', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO galleries (id, name, description, created_at) VALUES (11, '락앤롤', '설명', CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO gallery_managers (gallery_id, user_id, role, created_at) "
                + "VALUES (10, 3, 'MANAGER', CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO gallery_managers (gallery_id, user_id, role, created_at) "
                + "VALUES (11, 4, 'MANAGER', CURRENT_TIMESTAMP)");
        entityManager.clear();
    }

    @Test
    void 해당_갤러리의_매니저는_MANAGER_역할을_가진다() {
        assertThat(adapter.findActor(3L, 10L, NOW))
                .contains(new GalleryActor(3L, UserRole.USER, GalleryRole.MANAGER, false));
    }

    @Test
    void 다른_갤러리의_매니저는_이_갤러리에서_역할이_없다() {
        assertThat(adapter.findActor(4L, 10L, NOW))
                .contains(new GalleryActor(4L, UserRole.USER, null, true));
    }

    @Test
    void 일반_유저와_어드민의_서비스_역할을_구분한다() {
        assertThat(adapter.findActor(1L, 10L, NOW)).contains(new GalleryActor(1L, UserRole.USER, null, false));
        assertThat(adapter.findActor(9L, 10L, NOW)).contains(new GalleryActor(9L, UserRole.ADMIN, null, false));
    }

    @Test
    void 없거나_탈퇴한_사용자는_빈_값이다() {
        assertThat(adapter.findActor(404L, 10L, NOW)).isEmpty();
        assertThat(adapter.findActor(7L, 10L, NOW)).isEmpty();
    }

    @Test
    void 갤러리_존재_여부를_확인한다() {
        assertThat(adapter.existsGallery(10L)).isTrue();
        assertThat(adapter.existsGallery(404L)).isFalse();
    }

    private void insertUser(long id, String role, String deletedAt) {
        jdbcTemplate.update("INSERT INTO users (id, email, role, deleted_at, created_at) VALUES (?, ?, ?, "
                + (deletedAt == null ? "NULL" : deletedAt) + ", CURRENT_TIMESTAMP)", id, "u" + id + "@test.com", role);
    }
}
