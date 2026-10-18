package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.GalleryProposalItem;
import glitched.adlips.application.community.port.out.GalleryProposalPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.community.GalleryProposalStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class GetMyGalleryProposalsUseCaseTest {

    @Test
    void 내_개설_요청_목록을_읽기_전용_트랜잭션으로_조회한다() {
        GalleryProposalPort port = mock(GalleryProposalPort.class);
        TransactionRunner transactionRunner = mock(TransactionRunner.class);
        when(transactionRunner.readOnly(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        GalleryProposalItem item = new GalleryProposalItem(
                50L, 1L, "재즈", "재즈 이야기", GalleryProposalStatus.PENDING, null,
                LocalDateTime.of(2026, 10, 7, 12, 0), null);
        when(port.findByProposer(1L)).thenReturn(List.of(item));

        List<GalleryProposalItem> result = new GetMyGalleryProposalsUseCase(port, transactionRunner).execute(1L);

        assertThat(result).containsExactly(item);
        verify(transactionRunner).readOnly(any());
    }
}
