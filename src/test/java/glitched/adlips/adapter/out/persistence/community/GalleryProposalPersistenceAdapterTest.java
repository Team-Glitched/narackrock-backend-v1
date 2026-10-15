package glitched.adlips.adapter.out.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.community.port.out.GalleryProposalItem;
import glitched.adlips.domain.community.GalleryProposal;
import glitched.adlips.domain.community.GalleryProposalStatus;
import glitched.adlips.domain.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Lock;
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
    void 잠금_조회한_요청을_승인하고_저장하면_검토_정보가_반영된다() {
        Long proposalId = adapter.save(1L, "재즈", "설명");
        entityManager.flush();
        entityManager.clear();

        GalleryProposal proposal = adapter.findByIdForUpdate(proposalId).orElseThrow();
        proposal.approve(entityManager.getReference(User.class, 2L), LocalDateTime.of(2026, 10, 7, 12, 0));
        adapter.update(proposal);
        entityManager.flush();
        entityManager.clear();

        GalleryProposalItem item = adapter.findByProposer(1L).getFirst();
        assertThat(item.status()).isEqualTo(GalleryProposalStatus.APPROVED);
        assertThat(item.reviewedAt()).isEqualTo(LocalDateTime.of(2026, 10, 7, 12, 0));
        assertThat(adapter.findByIdForUpdate(999L)).isEmpty();
    }

    @Test
    void 잠금_조회는_비관적_쓰기_잠금을_사용한다() throws NoSuchMethodException {
        Lock lock = GalleryProposalJpaRepository.class.getMethod("findByIdForUpdate", Long.class)
                .getAnnotation(Lock.class);

        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void 목록은_상태로_거르고_커서_이전_요청을_최신순으로_반환한다() {
        Long first = adapter.save(1L, "재즈", "설명");
        Long second = adapter.save(2L, "블루스", "설명");
        Long third = adapter.save(1L, "가요", "설명");
        jdbcTemplate.update("UPDATE gallery_proposals SET status = 'REJECTED' WHERE id = ?", second);
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.findPage(null, null, 10)).extracting(GalleryProposalItem::proposalId)
                .containsExactly(third, second, first);
        assertThat(adapter.findPage(GalleryProposalStatus.PENDING, null, 10))
                .extracting(GalleryProposalItem::proposalId).containsExactly(third, first);
        assertThat(adapter.findPage(null, third, 1)).extracting(GalleryProposalItem::proposalId)
                .containsExactly(second);
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
