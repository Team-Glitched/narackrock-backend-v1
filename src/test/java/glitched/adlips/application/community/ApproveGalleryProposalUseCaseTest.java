package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.GalleryManagementPort;
import glitched.adlips.application.community.port.out.GalleryProposalPort;
import glitched.adlips.application.community.port.out.GallerySubscriptionPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.community.GalleryProposal;
import glitched.adlips.domain.community.GalleryProposalStatus;
import glitched.adlips.domain.community.GalleryRole;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApproveGalleryProposalUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC);

    GalleryProposalPort proposalPort;
    GalleryManagementPort managementPort;
    final GallerySubscriptionPort subscriptions = mock(GallerySubscriptionPort.class);
    UserRepositoryPort userRepository;
    TransactionRunner transactionRunner;
    ApproveGalleryProposalUseCase useCase;
    List<String> calls;

    User admin = User.restore(9L, "admin@example.com", UserRole.ADMIN, null);
    User proposer = User.restore(1L, "user@example.com", UserRole.USER, null);

    @BeforeEach
    void setUp() {
        proposalPort = mock(GalleryProposalPort.class);
        managementPort = mock(GalleryManagementPort.class);
        userRepository = mock(UserRepositoryPort.class);
        transactionRunner = mock(TransactionRunner.class);
        calls = new ArrayList<>();
        lenient().when(transactionRunner.required(any())).thenAnswer(invocation -> {
            calls.add("tx-begin");
            Object result = invocation.<Supplier<?>>getArgument(0).get();
            calls.add("tx-end");
            return result;
        });
        lenient().when(userRepository.findById(9L)).thenReturn(Optional.of(admin));
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(proposer));
        useCase = new ApproveGalleryProposalUseCase(
                proposalPort, managementPort, subscriptions, userRepository, transactionRunner, CLOCK);
    }

    @Test
    void 승인하면_요청자를_매니저와_가입자로_같은_트랜잭션에_등록한다() {
        GalleryProposal proposal = GalleryProposal.submit(proposer, "재즈", "재즈 이야기");
        when(proposalPort.findByIdForUpdate(50L)).thenReturn(Optional.of(proposal));
        when(managementPort.createGallery("재즈", "재즈 이야기")).thenAnswer(invocation -> {
            calls.add("create-gallery");
            return 10L;
        });
        doAnswer(invocation -> {
            calls.add("add-manager");
            return null;
        }).when(managementPort).addManager(10L, 1L, GalleryRole.MANAGER);

        doAnswer(invocation -> {
            calls.add("auto-join");
            return null;
        })
                .when(subscriptions).saveSubscription(1L, 10L);

        GalleryProposalReviewResult result = useCase.execute(50L, 9L);

        assertThat(result.galleryId()).isEqualTo(10L);
        assertThat(result.status()).isEqualTo(GalleryProposalStatus.APPROVED);
        assertThat(proposal.getStatus()).isEqualTo(GalleryProposalStatus.APPROVED);
        assertThat(proposal.getReviewedAt()).isEqualTo(LocalDateTime.now(CLOCK));
        verify(proposalPort).update(proposal);
        verify(subscriptions).saveSubscription(1L, 10L);
        assertThat(calls).containsExactly("tx-begin", "create-gallery", "add-manager", "auto-join", "tx-end");
    }

    @Test
    void 어드민이_아니면_요청을_조회하기_전에_FORBIDDEN이_발생한다() {
        assertProposalError(() -> useCase.execute(50L, 1L), GalleryProposalErrorCode.FORBIDDEN);
        verify(proposalPort, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void 존재하지_않는_사용자도_FORBIDDEN이다() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertProposalError(() -> useCase.execute(50L, 404L), GalleryProposalErrorCode.FORBIDDEN);
    }

    @Test
    void 요청이_없으면_PROPOSAL_NOT_FOUND가_발생한다() {
        when(proposalPort.findByIdForUpdate(50L)).thenReturn(Optional.empty());

        assertProposalError(() -> useCase.execute(50L, 9L), GalleryProposalErrorCode.PROPOSAL_NOT_FOUND);
    }

    @Test
    void 이미_검토된_요청이면_PROPOSAL_ALREADY_REVIEWED가_발생한다() {
        GalleryProposal proposal = GalleryProposal.submit(proposer, "재즈", "설명");
        proposal.reject(admin, "사유", LocalDateTime.now(CLOCK));
        when(proposalPort.findByIdForUpdate(50L)).thenReturn(Optional.of(proposal));

        assertProposalError(() -> useCase.execute(50L, 9L), GalleryProposalErrorCode.PROPOSAL_ALREADY_REVIEWED);
        verify(managementPort, never()).createGallery(anyString(), anyString());
    }

    @Test
    void 같은_이름의_갤러리가_생겼으면_GALLERY_NAME_DUPLICATED가_발생한다() {
        GalleryProposal proposal = GalleryProposal.submit(proposer, "재즈", "설명");
        when(proposalPort.findByIdForUpdate(50L)).thenReturn(Optional.of(proposal));
        when(proposalPort.existsGalleryByName("재즈")).thenReturn(true);

        assertProposalError(() -> useCase.execute(50L, 9L), GalleryProposalErrorCode.GALLERY_NAME_DUPLICATED);
        verify(managementPort, never()).createGallery(anyString(), anyString());
        assertThat(proposal.getStatus()).isEqualTo(GalleryProposalStatus.PENDING);
    }

    @Test
    void 자동_가입_실패시_승인_상태를_확정하지_않는다() {
        GalleryProposal proposal = GalleryProposal.submit(proposer, "재즈", "설명");
        when(proposalPort.findByIdForUpdate(50L)).thenReturn(Optional.of(proposal));
        when(managementPort.createGallery("재즈", "설명")).thenReturn(10L);
        org.mockito.Mockito.doThrow(new IllegalStateException("subscription failed"))
                .when(subscriptions).saveSubscription(1L, 10L);
        assertThatThrownBy(() -> useCase.execute(50L, 9L)).isInstanceOf(IllegalStateException.class);
        assertThat(proposal.getStatus()).isEqualTo(GalleryProposalStatus.PENDING);
        verify(proposalPort, never()).update(any());
    }

    private static void assertProposalError(Runnable action, GalleryProposalErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(GalleryProposalApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
