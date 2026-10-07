package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.GalleryProposalItem;
import glitched.adlips.application.community.port.out.GalleryProposalPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.community.GalleryProposalStatus;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetGalleryProposalsUseCaseTest {

    GalleryProposalPort proposalPort;
    UserRepositoryPort userRepository;
    GetGalleryProposalsUseCase useCase;

    @BeforeEach
    void setUp() {
        proposalPort = mock(GalleryProposalPort.class);
        userRepository = mock(UserRepositoryPort.class);
        TransactionRunner transactionRunner = mock(TransactionRunner.class);
        lenient().when(transactionRunner.readOnly(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        lenient().when(userRepository.findById(9L))
                .thenReturn(Optional.of(User.restore(9L, "admin@example.com", UserRole.ADMIN, null)));
        lenient().when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.restore(1L, "user@example.com", UserRole.USER, null)));
        useCase = new GetGalleryProposalsUseCase(proposalPort, userRepository, transactionRunner);
    }

    @Test
    void 한_건_더_조회해_다음_커서를_계산한다() {
        when(proposalPort.findPage(GalleryProposalStatus.PENDING, null, 3))
                .thenReturn(List.of(item(30L), item(20L), item(10L)));

        GalleryProposalPage page = useCase.execute(9L, "PENDING", null, 2);

        assertThat(page.proposals()).extracting(GalleryProposalItem::proposalId).containsExactly(30L, 20L);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextCursor()).isEqualTo(20L);
    }

    @Test
    void 마지막_페이지면_다음_커서가_없다() {
        when(proposalPort.findPage(null, 20L, 21)).thenReturn(List.of(item(10L)));

        GalleryProposalPage page = useCase.execute(9L, null, 20L, 20);

        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void 어드민이_아니면_FORBIDDEN이_발생한다() {
        assertProposalError(() -> useCase.execute(1L, null, null, 20), GalleryProposalErrorCode.FORBIDDEN);
        verify(proposalPort, never()).findPage(any(), any(), anyInt());
    }

    @Test
    void 상태값이나_페이지_값이_잘못되면_VALIDATION_ERROR가_발생한다() {
        assertProposalError(() -> useCase.execute(9L, "WRONG", null, 20), GalleryProposalErrorCode.VALIDATION_ERROR);
        assertProposalError(() -> useCase.execute(9L, null, 0L, 20), GalleryProposalErrorCode.VALIDATION_ERROR);
        assertProposalError(() -> useCase.execute(9L, null, null, 0), GalleryProposalErrorCode.VALIDATION_ERROR);
        assertProposalError(() -> useCase.execute(9L, null, null, 101), GalleryProposalErrorCode.VALIDATION_ERROR);
    }

    private static GalleryProposalItem item(Long id) {
        return new GalleryProposalItem(id, 1L, "갤러리" + id, "설명", GalleryProposalStatus.PENDING, null,
                LocalDateTime.of(2026, 10, 7, 12, 0), null);
    }

    private static void assertProposalError(Runnable action, GalleryProposalErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(GalleryProposalApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
