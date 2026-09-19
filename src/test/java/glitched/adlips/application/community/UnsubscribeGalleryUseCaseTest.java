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

class UnsubscribeGalleryUseCaseTest {

    GallerySubscriptionPort port;
    TransactionRunner transactionRunner;
    UnsubscribeGalleryUseCase useCase;

    @BeforeEach
    void setUp() {
        port = mock(GallerySubscriptionPort.class);
        transactionRunner = mock(TransactionRunner.class);
        lenient().when(transactionRunner.required(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        useCase = new UnsubscribeGalleryUseCase(port, transactionRunner);
    }

    @Test
    void 구독_취소는_required_트랜잭션으로_실행된다() {
        when(port.lockGallery(10L)).thenReturn(true);
        when(port.isSubscribed(1L, 10L)).thenReturn(true);

        useCase.execute(1L, 10L);

        verify(transactionRunner).required(any());
    }

    @Test
    void 구독_중이면_구독을_삭제한다() {
        when(port.lockGallery(10L)).thenReturn(true);
        when(port.isSubscribed(1L, 10L)).thenReturn(true);

        useCase.execute(1L, 10L);

        verify(port).deleteSubscription(1L, 10L);
    }

    @Test
    void 구독_중이_아니면_NOT_SUBSCRIBED가_발생한다() {
        when(port.lockGallery(10L)).thenReturn(true);
        when(port.isSubscribed(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(1L, 10L))
                .isInstanceOf(GalleryApplicationException.class)
                .hasMessage("구독 중인 갤러리가 아닙니다.")
                .extracting("errorCode")
                .isEqualTo(GalleryErrorCode.NOT_SUBSCRIBED);
        verify(port, never()).deleteSubscription(1L, 10L);
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
}
