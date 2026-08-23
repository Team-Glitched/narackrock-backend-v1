package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.PostQueryItem;
import glitched.adlips.application.community.port.out.PostQueryPort;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetPostUseCaseTest {

    PostQueryPort port;
    GetPostUseCase useCase;

    @BeforeEach
    void setUp() {
        port = mock(PostQueryPort.class);
        useCase = new GetPostUseCase(port);
    }

    @Test
    void 정상_조회시_결과를_반환한다() {
        PostQueryItem item = new PostQueryItem(
                501L, 10L, 1L, "writer", null, "제목", "내용", 0, 0, 0, 0, LocalDateTime.of(2026, 8, 23, 10, 0));
        when(port.existsGallery(10L)).thenReturn(true);
        when(port.findDetail(501L, 10L)).thenReturn(Optional.of(item));

        PostQueryItem result = useCase.execute(10L, 501L);

        assertThat(result).isEqualTo(item);
    }

    @Test
    void 존재하지_않는_갤러리면_GALLERY_NOT_FOUND가_발생한다() {
        when(port.existsGallery(999L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(999L, 501L))
                .isInstanceOf(PostApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(PostErrorCode.GALLERY_NOT_FOUND);
    }

    @Test
    void 존재하지_않는_게시글이면_POST_NOT_FOUND가_발생한다() {
        when(port.existsGallery(10L)).thenReturn(true);
        when(port.findDetail(999L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(10L, 999L))
                .isInstanceOf(PostApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(PostErrorCode.POST_NOT_FOUND);
    }
}
