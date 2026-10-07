package glitched.adlips.adapter.out.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.community.port.out.GalleryProposalItem;
import glitched.adlips.domain.community.GalleryProposalStatus;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class GalleryProposalPersistenceAdapterTest {

    @Autowired GalleryProposalJpaRepository proposalRepository;
    @Autowired GalleryJpaRepository galleryRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    GalleryProposalPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new GalleryProposalPersistenceAdapter(proposalRepository, galleryRepository, entityManager);

        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (1, 'one@test.com', 'USER', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO users (id, email, role, created_at) VALUES (2, 'two@test.com', 'USER', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO galleries (id, name, description, created_at) VALUES (10, '기타', '설명', CURRENT_TIMESTAMP)");
        entityManager.clear();
    }

    @Test
    void 저장한_요청은_대기_상태로_내_목록에_보인다() {
        Long proposalId = adapter.save(1L, "재즈", "재즈 이야기");
        entityManager.flush();
        entityManager.clear();

        List<GalleryProposalItem> items = adapter.findByProposer(1L);

        assertThat(items).hasSize(1);
        GalleryProposalItem item = items.getFirst();
        assertThat(item.proposalId()).isEqualTo(proposalId);
        assertThat(item.proposerId()).isEqualTo(1L);
        assertThat(item.name()).isEqualTo("재즈");
        assertThat(item.status()).isEqualTo(GalleryProposalStatus.PENDING);
        assertThat(item.createdAt()).isNotNull();
        assertThat(adapter.findByProposer(2L)).isEmpty();
    }

    @Test
    void 내_목록은_최신_요청부터_정렬된다() {
        Long first = adapter.save(1L, "재즈", "설명");
        Long second = adapter.save(1L, "블루스", "설명");
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.findByProposer(1L)).extracting(GalleryProposalItem::proposalId)
                .containsExactly(second, first);
    }

    @Test
    void 갤러리_이름_존재_여부를_확인한다() {
        assertThat(adapter.existsGalleryByName("기타")).isTrue();
        assertThat(adapter.existsGalleryByName("재즈")).isFalse();
    }

    @Test
    void 대기_중인_요청의_이름과_개수를_확인한다() {
        adapter.save(1L, "재즈", "설명");
        jdbcTemplate.update("INSERT INTO gallery_proposals (user_id, name, description, status, created_at) "
                + "VALUES (1, '락앤롤', '설명', 'REJECTED', CURRENT_TIMESTAMP)");
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.existsPendingByName("재즈")).isTrue();
        assertThat(adapter.existsPendingByName("락앤롤")).isFalse();
        assertThat(adapter.countPendingByProposer(1L)).isEqualTo(1L);
        assertThat(adapter.countPendingByProposer(2L)).isZero();
    }
}
