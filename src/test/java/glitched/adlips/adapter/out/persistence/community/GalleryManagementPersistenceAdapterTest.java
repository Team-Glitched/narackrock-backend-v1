package glitched.adlips.adapter.out.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import glitched.adlips.application.community.port.out.GalleryQueryItem;
import glitched.adlips.domain.community.GalleryRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class GalleryManagementPersistenceAdapterTest {

    @Autowired GalleryJpaRepository galleryRepository;
    @Autowired GalleryManagerJpaRepository managerRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    GalleryManagementPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new GalleryManagementPersistenceAdapter(galleryRepository, managerRepository, entityManager);
        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (1, 'one@test.com', 'USER', CURRENT_TIMESTAMP)");
        entityManager.clear();
    }

    @Test
    void 갤러리를_만들고_매니저를_지정한다() {
        Long galleryId = adapter.createGallery("재즈", "재즈 이야기");
        adapter.addManager(galleryId, 1L, GalleryRole.MANAGER);
        entityManager.flush();
        entityManager.clear();

        assertThat(galleryRepository.findById(galleryId)).get()
                .satisfies(gallery -> {
                    assertThat(gallery.getName()).isEqualTo("재즈");
                    assertThat(gallery.getDescription()).isEqualTo("재즈 이야기");
                });
        assertThat(jdbcTemplate.queryForObject(
                "SELECT role FROM gallery_managers WHERE gallery_id = ? AND user_id = 1", String.class, galleryId))
                .isEqualTo("MANAGER");
    }

    @Test
    void 만든_갤러리는_갤러리_목록_조회에_나타난다() {
        Long galleryId = adapter.createGallery("재즈", "재즈 이야기");
        entityManager.flush();
        entityManager.clear();

        assertThat(new GalleryQueryPersistenceAdapter(entityManager).findGalleries())
                .extracting(GalleryQueryItem::galleryId, GalleryQueryItem::name, GalleryQueryItem::subscriberCount)
                .containsExactly(tuple(galleryId, "재즈", 0L));
    }

    @Test
    void 같은_갤러리에_같은_사용자를_두_번_지정할_수_없다() {
        Long galleryId = adapter.createGallery("재즈", "재즈 이야기");
        adapter.addManager(galleryId, 1L, GalleryRole.MANAGER);
        entityManager.flush();

        assertThatThrownBy(() -> {
            adapter.addManager(galleryId, 1L, GalleryRole.MANAGER);
            entityManager.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class, org.hibernate.exception.ConstraintViolationException.class);
    }
}
