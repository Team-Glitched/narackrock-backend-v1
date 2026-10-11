package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.GallerySubscriptionPort;
import glitched.adlips.application.port.TransactionRunner;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscribeGalleryUseCaseTest {

    GallerySubscriptionPort port;
    TransactionRunner transactionRunner;
    SubscribeGalleryUseCase useCase;

    @BeforeEach
    void setUp() {
        port = mock(GallerySubscriptionPort.class);
        transactionRunner = mock(TransactionRunner.class);
        lenient().when(transactionRunner.required(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        useCase = new SubscribeGalleryUseCase(port, transactionRunner);
    }

    @Test
    void 구독은_required_트랜잭션으로_실행된다() {
        when(port.lockGallery(10L)).thenReturn(true);
        when(port.isSubscribed(1L, 10L)).thenReturn(false);

        useCase.execute(1L, 10L);

        verify(transactionRunner).required(any());
    }

    @Test
    void 구독하지_않은_갤러리면_구독을_저장한다() {
        when(port.lockGallery(10L)).thenReturn(true);
        when(port.isSubscribed(1L, 10L)).thenReturn(false);

        useCase.execute(1L, 10L);

        verify(port).saveSubscription(1L, 10L);
    }

    @Test
    void 이미_구독_중이면_ALREADY_SUBSCRIBED가_발생한다() {
        when(port.lockGallery(10L)).thenReturn(true);
        when(port.isSubscribed(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(1L, 10L))
                .isInstanceOf(GalleryApplicationException.class)
                .hasMessage("이미 구독 중인 갤러리입니다.")
                .extracting("errorCode")
                .isEqualTo(GalleryErrorCode.ALREADY_SUBSCRIBED);
        verify(port, never()).saveSubscription(1L, 10L);
    }

    @Test
    void 존재하지_않는_갤러리면_GALLERY_NOT_FOUND가_발생한다() {
        when(port.lockGallery(999L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(1L, 999L))
                .isInstanceOf(GalleryApplicationException.class)
                .hasMessage("존재하지 않거나 삭제된 갤러리입니다.")
                .extracting("errorCode")
                .isEqualTo(GalleryErrorCode.GALLERY_NOT_FOUND);
        verify(port, never()).isSubscribed(1L, 999L);
    }

    @Test
    void 갤러리_행을_먼저_잠근_뒤_구독_여부를_확인한다() {
        when(port.lockGallery(10L)).thenReturn(true);
        when(port.isSubscribed(1L, 10L)).thenReturn(false);

        useCase.execute(1L, 10L);

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(port);
        order.verify(port).lockGallery(10L);
        order.verify(port).isSubscribed(1L, 10L);
        order.verify(port).saveSubscription(1L, 10L);
    }
}
