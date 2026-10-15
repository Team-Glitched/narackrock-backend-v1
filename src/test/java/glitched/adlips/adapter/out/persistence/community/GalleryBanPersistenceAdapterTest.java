package glitched.adlips.adapter.out.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.community.port.out.GalleryBanItem;
import glitched.adlips.domain.community.Gallery;
import glitched.adlips.domain.community.GalleryBan;
import glitched.adlips.domain.community.GalleryBanType;
import glitched.adlips.domain.user.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class GalleryBanPersistenceAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 12, 0);

    @Autowired GalleryBanJpaRepository banRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    GalleryBanPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new GalleryBanPersistenceAdapter(banRepository);
        for (long id : new long[] {1, 2, 3}) {
            jdbcTemplate.update("INSERT INTO users (id, email, role, created_at) VALUES (?, ?, 'USER', CURRENT_TIMESTAMP)",
                    id, "u" + id + "@test.com");
        }
        jdbcTemplate.update(
                "INSERT INTO galleries (id, name, description, created_at) VALUES (10, '재즈', '설명', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO galleries (id, name, description, created_at) VALUES (11, '락앤롤', '설명', CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO posts (id, gallery_id, user_id, title, content, view_count, like_count, "
                + "dislike_count, comment_count, created_at) VALUES (100, 10, 2, '제목', '내용', 0, 0, 0, 0, CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO post_comments (id, post_id, user_id, content, reply_count, like_count, "
                + "dislike_count, created_at) VALUES (200, 100, 2, '댓글', 0, 0, 0, CURRENT_TIMESTAMP)");
        entityManager.clear();
    }

    @Test
    void 이용정지는_만료_전까지_해당_갤러리에서만_제한된다() {
        adapter.save(GalleryBan.suspend(gallery(10L), user(1L), user(3L), "도배", 1, NOW));
        flush();

        assertThat(adapter.existsActive(10L, 1L, NOW)).isTrue();
        assertThat(adapter.isRestrictedInGallery(10L, 1L, NOW)).isTrue();
        assertThat(adapter.isRestrictedInGallery(10L, 1L, NOW.plusDays(1))).isFalse();
        assertThat(adapter.isRestrictedInGallery(11L, 1L, NOW)).isFalse();
        assertThat(adapter.isRestrictedInGallery(10L, 2L, NOW)).isFalse();
        assertThat(adapter.isExpelled(10L, 1L, NOW)).isFalse();
    }

    @Test
    void 게시글과_댓글로_갤러리를_찾아_제한을_판정한다() {
        adapter.save(GalleryBan.expel(gallery(10L), user(1L), user(3L), "광고"));
        flush();

        assertThat(adapter.isRestrictedForPost(100L, 1L, NOW)).isTrue();
        assertThat(adapter.isRestrictedForComment(200L, 1L, NOW)).isTrue();
        assertThat(adapter.isRestrictedForPost(100L, 2L, NOW)).isFalse();
        assertThat(adapter.isRestrictedForPost(999L, 1L, NOW)).isFalse();
        assertThat(adapter.isExpelled(10L, 1L, NOW)).isTrue();
    }

    @Test
    void 해제된_제재는_효력이_없다() {
        Long banId = adapter.save(GalleryBan.expel(gallery(10L), user(1L), user(3L), "광고"));
        flush();
        GalleryBan ban = adapter.findById(banId).orElseThrow();
        ban.lift(user(3L), NOW);
        adapter.update(ban);
        flush();

        assertThat(adapter.existsActive(10L, 1L, NOW)).isFalse();
        assertThat(adapter.isExpelled(10L, 1L, NOW)).isFalse();
        assertThat(adapter.findById(banId)).get().extracting(GalleryBan::getLiftedById).isEqualTo(3L);
    }

    @Test
    void 목록은_갤러리별_최신순이며_활성만_거를_수_있다() {
        Long expired = adapter.save(GalleryBan.suspend(gallery(10L), user(1L), user(3L), "도배", 1, NOW.minusDays(5)));
        Long active = adapter.save(GalleryBan.expel(gallery(10L), user(2L), user(3L), "광고"));
        adapter.save(GalleryBan.expel(gallery(11L), user(1L), user(3L), "광고"));
        flush();

        assertThat(adapter.findPage(10L, false, NOW, null, 10)).extracting(GalleryBanItem::banId)
                .containsExactly(active, expired);
        assertThat(adapter.findPage(10L, true, NOW, null, 10)).extracting(GalleryBanItem::banId)
                .containsExactly(active);
        assertThat(adapter.findPage(10L, false, NOW, active, 10)).extracting(GalleryBanItem::banId)
                .containsExactly(expired);

        GalleryBanItem item = adapter.findPage(10L, false, NOW, null, 10).getFirst();
        assertThat(item.userId()).isEqualTo(2L);
        assertThat(item.actorId()).isEqualTo(3L);
        assertThat(item.banType()).isEqualTo(GalleryBanType.EXPEL);
        assertThat(item.active()).isTrue();
        assertThat(adapter.findPage(10L, false, NOW, null, 10).getLast().active()).isFalse();
    }

    private Gallery gallery(Long id) {
        return entityManager.getReference(Gallery.class, id);
    }

    private User user(Long id) {
        return entityManager.getReference(User.class, id);
    }

    private void flush() {
        entityManager.flush();
        entityManager.clear();
    }
}
