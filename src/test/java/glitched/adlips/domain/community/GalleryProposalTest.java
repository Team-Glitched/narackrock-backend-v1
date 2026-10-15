package glitched.adlips.domain.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserRole;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class GalleryProposalTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 12, 0);

    private User proposer() {
        return User.create("proposer@example.com").withId(1L);
    }

    private User admin() {
        return User.restore(9L, "admin@example.com", UserRole.ADMIN, null);
    }

    @Test
    void 제출하면_이름과_설명을_다듬어_대기_상태로_보관한다() {
        GalleryProposal proposal = GalleryProposal.submit(proposer(), "  재즈 갤러리  ", "  재즈 이야기  ");

        assertThat(proposal.getProposerId()).isEqualTo(1L);
        assertThat(proposal.getName()).isEqualTo("재즈 갤러리");
        assertThat(proposal.getDescription()).isEqualTo("재즈 이야기");
        assertThat(proposal.getStatus()).isEqualTo(GalleryProposalStatus.PENDING);
        assertThat(proposal.getReviewedAt()).isNull();
    }

    @Test
    void 이름이_2자_미만이거나_30자를_넘으면_예외가_발생한다() {
        assertThatThrownBy(() -> GalleryProposal.submit(proposer(), "재", "설명"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GalleryProposal.submit(proposer(), "가".repeat(31), "설명"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 이름이나_설명이_비어_있으면_예외가_발생한다() {
        assertThatThrownBy(() -> GalleryProposal.submit(proposer(), null, "설명"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GalleryProposal.submit(proposer(), "재즈", "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 설명이_500자를_넘으면_예외가_발생한다() {
        assertThatThrownBy(() -> GalleryProposal.submit(proposer(), "재즈", "가".repeat(501)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 제출자가_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> GalleryProposal.submit(null, "재즈", "설명"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void 승인하면_검토자와_시각을_기록한다() {
        GalleryProposal proposal = GalleryProposal.submit(proposer(), "재즈", "설명");

        proposal.approve(admin(), NOW);

        assertThat(proposal.getStatus()).isEqualTo(GalleryProposalStatus.APPROVED);
        assertThat(proposal.getReviewedById()).isEqualTo(9L);
        assertThat(proposal.getReviewedAt()).isEqualTo(NOW);
    }

    @Test
    void 반려하면_사유를_다듬어_기록한다() {
        GalleryProposal proposal = GalleryProposal.submit(proposer(), "재즈", "설명");

        proposal.reject(admin(), "  중복 주제  ", NOW);

        assertThat(proposal.getStatus()).isEqualTo(GalleryProposalStatus.REJECTED);
        assertThat(proposal.getRejectionReason()).isEqualTo("중복 주제");
        assertThat(proposal.getReviewedAt()).isEqualTo(NOW);
    }

    @Test
    void 반려_사유가_없으면_예외가_발생한다() {
        GalleryProposal proposal = GalleryProposal.submit(proposer(), "재즈", "설명");

        assertThatThrownBy(() -> proposal.reject(admin(), " ", NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(proposal.getStatus()).isEqualTo(GalleryProposalStatus.PENDING);
    }

    @Test
    void 이미_검토된_요청은_다시_승인하거나_반려할_수_없다() {
        GalleryProposal approved = GalleryProposal.submit(proposer(), "재즈", "설명");
        approved.approve(admin(), NOW);
        GalleryProposal rejected = GalleryProposal.submit(proposer(), "락앤롤", "설명");
        rejected.reject(admin(), "사유", NOW);

        assertThatThrownBy(() -> approved.approve(admin(), NOW)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> approved.reject(admin(), "사유", NOW)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> rejected.approve(admin(), NOW)).isInstanceOf(IllegalStateException.class);
    }
}
