package glitched.adlips.adapter.out.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.community.port.out.GalleryQueryItem;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class GallerySubscriptionPersistenceAdapterTest {

    @Autowired GalleryJpaRepository galleryRepository;
    @Autowired GallerySubscriptionJpaRepository subscriptionRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    GallerySubscriptionPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new GallerySubscriptionPersistenceAdapter(galleryRepository, subscriptionRepository);

        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (1, 'one@test.com', 'USER', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO galleries (id, name, description, created_at) VALUES (10, '기타', '설명', CURRENT_TIMESTAMP)");
        entityManager.clear();
    }

    @Test
    void 존재하는_갤러리는_잠그고_true를_반환한다() {
        assertThat(adapter.lockGallery(10L)).isTrue();
    }

    @Test
    void 존재하지_않는_갤러리는_false를_반환한다() {
        assertThat(adapter.lockGallery(999L)).isFalse();
    }

    @Test
    void 구독_전에는_isSubscribed가_false다() {
        assertThat(adapter.isSubscribed(1L, 10L)).isFalse();
    }

    @Test
    void 구독을_저장하면_isSubscribed가_true가_된다() {
        adapter.saveSubscription(1L, 10L);
        entityManager.clear();

        assertThat(adapter.isSubscribed(1L, 10L)).isTrue();
    }

    @Test
    void 구독을_삭제하면_isSubscribed가_다시_false가_된다() {
        adapter.saveSubscription(1L, 10L);
        entityManager.clear();

        adapter.deleteSubscription(1L, 10L);
        entityManager.clear();

        assertThat(adapter.isSubscribed(1L, 10L)).isFalse();
    }

    @Test
    void 저장한_구독은_가입_목록_조회에_그대로_반영된다() {
        adapter.saveSubscription(1L, 10L);
        entityManager.clear();

        GalleryQueryPersistenceAdapter queryAdapter = new GalleryQueryPersistenceAdapter(entityManager);
        List<GalleryQueryItem> subscribed = queryAdapter.findSubscribedGalleries(1L);

        assertThat(subscribed).extracting(GalleryQueryItem::galleryId).containsExactly(10L);
        assertThat(subscribed.getFirst().subscriberCount()).isEqualTo(1L);
    }

    @Test
    void 삭제한_구독은_가입_목록에서도_사라진다() {
        adapter.saveSubscription(1L, 10L);
        entityManager.clear();
        adapter.deleteSubscription(1L, 10L);
        entityManager.clear();

        GalleryQueryPersistenceAdapter queryAdapter = new GalleryQueryPersistenceAdapter(entityManager);

        assertThat(queryAdapter.findSubscribedGalleries(1L)).isEmpty();
    }

    @Test
    void 다른_사용자의_구독은_영향받지_않는다() {
        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (2, 'two@test.com', 'USER', CURRENT_TIMESTAMP)");
        adapter.saveSubscription(1L, 10L);
        adapter.saveSubscription(2L, 10L);
        entityManager.clear();

        adapter.deleteSubscription(1L, 10L);
        entityManager.clear();

        assertThat(adapter.isSubscribed(1L, 10L)).isFalse();
        assertThat(adapter.isSubscribed(2L, 10L)).isTrue();
    }
}
