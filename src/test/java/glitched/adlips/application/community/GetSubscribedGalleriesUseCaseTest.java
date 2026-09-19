package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.GalleryQueryItem;
import glitched.adlips.application.community.port.out.GalleryQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetSubscribedGalleriesUseCaseTest {

    GalleryQueryPort port;
    TransactionRunner transactionRunner;
    GetSubscribedGalleriesUseCase useCase;

    @BeforeEach
    void setUp() {
        port = mock(GalleryQueryPort.class);
        transactionRunner = mock(TransactionRunner.class);
        lenient().when(transactionRunner.readOnly(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        useCase = new GetSubscribedGalleriesUseCase(port, transactionRunner);
    }

    @Test
    void 조회는_readOnly_트랜잭션으로_실행된다() {
        when(port.findSubscribedGalleries(1L)).thenReturn(List.of());

        useCase.execute(1L);

        verify(transactionRunner).readOnly(any());
    }

    @Test
    void 요청자의_가입_갤러리만_포트에_조회한다() {
        GalleryQueryItem item = new GalleryQueryItem(
                1L, "기타", "기타 연주와 작곡을 다루는 갤러리입니다.", 128L, LocalDateTime.of(2026, 6, 22, 12, 0));
        when(port.findSubscribedGalleries(7L)).thenReturn(List.of(item));

        List<GalleryQueryItem> result = useCase.execute(7L);

        assertThat(result).containsExactly(item);
        verify(port).findSubscribedGalleries(7L);
    }

    @Test
    void 가입한_갤러리가_없으면_빈_목록을_반환한다() {
        when(port.findSubscribedGalleries(7L)).thenReturn(List.of());

        assertThat(useCase.execute(7L)).isEmpty();
    }
}
