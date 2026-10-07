package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.GalleryProposalPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.community.GalleryProposal;
import glitched.adlips.domain.community.GalleryProposalStatus;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RejectGalleryProposalUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC);

    GalleryProposalPort proposalPort;
    UserRepositoryPort userRepository;
    TransactionRunner transactionRunner;
    RejectGalleryProposalUseCase useCase;

    User admin = User.restore(9L, "admin@example.com", UserRole.ADMIN, null);
    User proposer = User.restore(1L, "user@example.com", UserRole.USER, null);

    @BeforeEach
    void setUp() {
        proposalPort = mock(GalleryProposalPort.class);
        userRepository = mock(UserRepositoryPort.class);
        transactionRunner = mock(TransactionRunner.class);
        lenient().when(transactionRunner.required(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        lenient().when(userRepository.findById(9L)).thenReturn(Optional.of(admin));
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(proposer));
        useCase = new RejectGalleryProposalUseCase(proposalPort, userRepository, transactionRunner, CLOCK);
    }

    @Test
    void 반려하면_사유와_검토_정보를_기록한다() {
        GalleryProposal proposal = GalleryProposal.submit(proposer, "재즈", "설명");
        when(proposalPort.findByIdForUpdate(50L)).thenReturn(Optional.of(proposal));

        GalleryProposalReviewResult result = useCase.execute(50L, 9L, "  중복 주제 ");

        assertThat(result.status()).isEqualTo(GalleryProposalStatus.REJECTED);
        assertThat(result.galleryId()).isNull();
        assertThat(result.reviewedAt()).isEqualTo(LocalDateTime.now(CLOCK));
        assertThat(proposal.getRejectionReason()).isEqualTo("중복 주제");
        verify(proposalPort).update(proposal);
        verify(transactionRunner).required(any());
    }

    @Test
    void 어드민이_아니면_FORBIDDEN이_발생한다() {
        assertProposalError(() -> useCase.execute(50L, 1L, "사유"), GalleryProposalErrorCode.FORBIDDEN);
        verify(proposalPort, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void 사유가_없으면_VALIDATION_ERROR가_발생한다() {
        assertProposalError(() -> useCase.execute(50L, 9L, "  "), GalleryProposalErrorCode.VALIDATION_ERROR);
        assertProposalError(() -> useCase.execute(50L, 9L, null), GalleryProposalErrorCode.VALIDATION_ERROR);
        verify(proposalPort, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void 요청이_없으면_PROPOSAL_NOT_FOUND가_발생한다() {
        when(proposalPort.findByIdForUpdate(50L)).thenReturn(Optional.empty());

        assertProposalError(() -> useCase.execute(50L, 9L, "사유"), GalleryProposalErrorCode.PROPOSAL_NOT_FOUND);
    }

    @Test
    void 이미_검토된_요청이면_PROPOSAL_ALREADY_REVIEWED가_발생한다() {
        GalleryProposal proposal = GalleryProposal.submit(proposer, "재즈", "설명");
        proposal.approve(admin, LocalDateTime.now(CLOCK));
        when(proposalPort.findByIdForUpdate(50L)).thenReturn(Optional.of(proposal));

        assertProposalError(() -> useCase.execute(50L, 9L, "사유"),
                GalleryProposalErrorCode.PROPOSAL_ALREADY_REVIEWED);
        verify(proposalPort, never()).update(any());
    }

    private static void assertProposalError(Runnable action, GalleryProposalErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(GalleryProposalApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
