package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.GalleryProposalPort;
import glitched.adlips.application.community.port.out.UserBanQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubmitGalleryProposalUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC);

    GalleryProposalPort port;
    UserBanQueryPort userBanQueryPort;
    TransactionRunner transactionRunner;
    SubmitGalleryProposalUseCase useCase;

    @BeforeEach
    void setUp() {
        port = mock(GalleryProposalPort.class);
        userBanQueryPort = mock(UserBanQueryPort.class);
        transactionRunner = mock(TransactionRunner.class);
        lenient().when(transactionRunner.required(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        useCase = new SubmitGalleryProposalUseCase(port, userBanQueryPort, transactionRunner, CLOCK);
    }

    @Test
    void 다듬은_이름과_설명으로_개설_요청을_저장한다() {
        when(port.save(1L, "재즈", "재즈 이야기")).thenReturn(50L);

        Long proposalId = useCase.execute(1L, "  재즈  ", "  재즈 이야기 ");

        assertThat(proposalId).isEqualTo(50L);
        verify(transactionRunner).required(any());
    }

    @Test
    void 서비스_전역_정지_사용자는_BANNED_USER_ACCESS가_발생한다() {
        when(userBanQueryPort.isBanned(1L, LocalDateTime.now(CLOCK))).thenReturn(true);

        assertProposalError(() -> useCase.execute(1L, "재즈", "설명"), GalleryProposalErrorCode.BANNED_USER_ACCESS);
        verify(port, never()).save(anyLong(), anyString(), anyString());
    }

    @Test
    void 이름이나_설명이_규칙을_어기면_VALIDATION_ERROR가_발생한다() {
        assertProposalError(() -> useCase.execute(1L, "재", "설명"), GalleryProposalErrorCode.VALIDATION_ERROR);
        assertProposalError(() -> useCase.execute(1L, "재즈", " "), GalleryProposalErrorCode.VALIDATION_ERROR);
        assertProposalError(() -> useCase.execute(1L, null, "설명"), GalleryProposalErrorCode.VALIDATION_ERROR);
        verify(port, never()).save(anyLong(), anyString(), anyString());
    }

    @Test
    void 같은_이름의_갤러리가_있으면_GALLERY_NAME_DUPLICATED가_발생한다() {
        when(port.existsGalleryByName("재즈")).thenReturn(true);

        assertProposalError(() -> useCase.execute(1L, "재즈", "설명"), GalleryProposalErrorCode.GALLERY_NAME_DUPLICATED);
        verify(port, never()).save(anyLong(), anyString(), anyString());
    }

    @Test
    void 같은_이름의_대기_요청이_있으면_PROPOSAL_NAME_DUPLICATED가_발생한다() {
        when(port.existsPendingByName("재즈")).thenReturn(true);

        assertProposalError(() -> useCase.execute(1L, "재즈", "설명"), GalleryProposalErrorCode.PROPOSAL_NAME_DUPLICATED);
        verify(port, never()).save(anyLong(), anyString(), anyString());
    }

    @Test
    void 대기_중인_요청이_이미_있으면_TOO_MANY_PENDING_PROPOSALS가_발생한다() {
        when(port.countPendingByProposer(1L)).thenReturn(1L);

        assertProposalError(() -> useCase.execute(1L, "재즈", "설명"),
                GalleryProposalErrorCode.TOO_MANY_PENDING_PROPOSALS);
        verify(port, never()).save(anyLong(), anyString(), anyString());
    }

    private static void assertProposalError(Runnable action, GalleryProposalErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(GalleryProposalApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
