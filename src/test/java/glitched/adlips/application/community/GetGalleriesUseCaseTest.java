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

class GetGalleriesUseCaseTest {

    GalleryQueryPort port;
    TransactionRunner transactionRunner;
    GetGalleriesUseCase useCase;

    @BeforeEach
    void setUp() {
        port = mock(GalleryQueryPort.class);
        transactionRunner = mock(TransactionRunner.class);
        lenient().when(transactionRunner.readOnly(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        useCase = new GetGalleriesUseCase(port, transactionRunner);
    }

    @Test
    void 조회는_readOnly_트랜잭션으로_실행된다() {
        when(port.findGalleries()).thenReturn(List.of());

        useCase.execute();

        verify(transactionRunner).readOnly(any());
    }

    @Test
    void 포트가_반환한_갤러리_목록을_그대로_반환한다() {
        GalleryQueryItem item = new GalleryQueryItem(
                1L, "기타", "기타 연주와 작곡을 다루는 갤러리입니다.", 128L, LocalDateTime.of(2026, 6, 22, 12, 0));
        when(port.findGalleries()).thenReturn(List.of(item));

        List<GalleryQueryItem> result = useCase.execute();

        assertThat(result).containsExactly(item);
    }

    @Test
    void 갤러리가_없으면_빈_목록을_반환한다() {
        when(port.findGalleries()).thenReturn(List.of());

        assertThat(useCase.execute()).isEmpty();
    }
}
