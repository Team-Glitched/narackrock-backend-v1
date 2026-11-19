package glitched.adlips.adapter.out.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.community.port.out.GalleryQueryItem;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class GalleryQueryPersistenceAdapterTest {

    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    GalleryQueryPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new GalleryQueryPersistenceAdapter(entityManager);

        insertUser(1L, "one@test.com");
        insertUser(2L, "two@test.com");
        insertUser(3L, "three@test.com");

        insertGallery(10L, "기타", "기타 연주와 작곡을 다루는 갤러리입니다.", "2026-06-22 12:00:00");
        insertGallery(20L, "드럼", null, "2026-06-23 12:00:00");
        insertGallery(30L, "베이스", "베이스 갤러리입니다.", "2026-06-24 12:00:00");

        // 10번과 30번은 구독자 2명으로 동률 — id 오름차순으로 갈린다.
        subscribe(1L, 10L);
        subscribe(2L, 10L);
        subscribe(1L, 30L);
        subscribe(3L, 30L);

        entityManager.clear();
    }

    @Test
    void 전체_목록은_구독자_수_내림차순_같으면_id_오름차순으로_반환한다() {
        List<GalleryQueryItem> galleries = adapter.findGalleries();

        assertThat(galleries).extracting(GalleryQueryItem::galleryId)
                .containsExactly(10L, 30L, 20L);
    }

    @Test
    void 전체_목록은_구독자_수를_집계해_반환한다() {
        List<GalleryQueryItem> galleries = adapter.findGalleries();

        assertThat(galleries).extracting(GalleryQueryItem::galleryId, GalleryQueryItem::subscriberCount)
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple(10L, 2L),
                        org.assertj.core.api.Assertions.tuple(30L, 2L),
                        org.assertj.core.api.Assertions.tuple(20L, 0L));
    }

    @Test
    void 전체_목록은_이름과_설명과_생성시각을_반환한다() {
        GalleryQueryItem first = adapter.findGalleries().getFirst();

        assertThat(first.name()).isEqualTo("기타");
        assertThat(first.description()).isEqualTo("기타 연주와 작곡을 다루는 갤러리입니다.");
        assertThat(first.createdAt()).isEqualTo(LocalDateTime.of(2026, 6, 22, 12, 0));
    }

    @Test
    void 설명이_없는_갤러리는_설명을_null로_반환한다() {
        GalleryQueryItem drum = adapter.findGalleries().stream()
                .filter(item -> item.galleryId() == 20L)
                .findFirst()
                .orElseThrow();

        assertThat(drum.description()).isNull();
    }

    @Test
    void 가입_목록은_요청자가_구독한_갤러리만_반환한다() {
        List<GalleryQueryItem> galleries = adapter.findSubscribedGalleries(2L);

        assertThat(galleries).extracting(GalleryQueryItem::galleryId).containsExactly(10L);
    }

    @Test
    void 가입_목록도_구독자_수_내림차순_같으면_id_오름차순으로_반환한다() {
        List<GalleryQueryItem> galleries = adapter.findSubscribedGalleries(1L);

        assertThat(galleries).extracting(GalleryQueryItem::galleryId).containsExactly(10L, 30L);
    }

    @Test
    void 가입_목록의_구독자_수는_요청자만이_아니라_전체_구독자_수다() {
        List<GalleryQueryItem> galleries = adapter.findSubscribedGalleries(2L);

        assertThat(galleries.getFirst().subscriberCount()).isEqualTo(2L);
    }

    @Test
    void 구독한_갤러리가_없으면_빈_목록을_반환한다() {
        assertThat(adapter.findSubscribedGalleries(99L)).isEmpty();
    }

    @Test
    void manageableListIncludesOnlyAssignmentsUnlessUserIsAnActiveAdmin() {
        jdbcTemplate.update("INSERT INTO gallery_managers (gallery_id, user_id, role, created_at) VALUES (20, 1, 'MANAGER', CURRENT_TIMESTAMP)");
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE id IN (2, 3)");
        jdbcTemplate.update("UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = 3");

        assertThat(adapter.findManageableGalleries(1L)).extracting(GalleryQueryItem::galleryId)
                .containsExactly(20L);
        assertThat(adapter.findManageableGalleries(2L)).extracting(GalleryQueryItem::galleryId)
                .containsExactly(10L, 30L, 20L);
        assertThat(adapter.findManageableGalleries(3L)).isEmpty();
    }

    @Test
    void 이미지와_배너가_모든_커뮤니티_목록에_반영된다() {
        for (long id : new long[]{100L,101L}) {
            jdbcTemplate.update("INSERT INTO media_files (id,owner_id,file_url,file_type,status,file_size,created_at,updated_at) VALUES (?,1,?,'IMAGE','READY',100,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",id,"https://files.test/"+id+".png");
        }
        jdbcTemplate.update("UPDATE galleries SET image_file_id=100,banner_image_file_id=101 WHERE id=10");
        jdbcTemplate.update("INSERT INTO gallery_managers (gallery_id,user_id,role,created_at) VALUES (10,1,'MANAGER',CURRENT_TIMESTAMP)");
        for (List<GalleryQueryItem> list : List.of(adapter.findGalleries(),adapter.findSubscribedGalleries(1L),adapter.findManageableGalleries(1L))) {
            GalleryQueryItem item=list.stream().filter(it->it.galleryId().equals(10L)).findFirst().orElseThrow();
            assertThat(item.imageFileId()).isEqualTo(100L);
            assertThat(item.bannerImageFileId()).isEqualTo(101L);
            assertThat(item.imageUrl()).isEqualTo("https://files.test/100.png");
            assertThat(item.bannerImageUrl()).isEqualTo("https://files.test/101.png");
            assertThat(item.subscriberCount()).isEqualTo(2L);
        }
    }

    private void insertUser(long id, String email) {
        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (?, ?, 'USER', CURRENT_TIMESTAMP)",
                id, email);
    }

    private void insertGallery(long id, String name, String description, String createdAt) {
        jdbcTemplate.update(
                "INSERT INTO galleries (id, name, description, created_at) VALUES (?, ?, ?, ?)",
                id, name, description, createdAt);
    }

    private void subscribe(long userId, long galleryId) {
        jdbcTemplate.update(
                "INSERT INTO gallery_subs (user_id, gallery_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
                userId, galleryId);
    }
}
