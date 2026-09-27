package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.PostQueryItem;
import glitched.adlips.application.community.port.out.PostQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetPopularPostsUseCaseTest {

    final PostQueryPort port = mock(PostQueryPort.class);
    final GetPopularPostsUseCase useCase = new GetPopularPostsUseCase(port, TransactionRunner.direct());

    @Test
    void 전체_갤러리의_인기글과_전체_수를_반환한다() {
        PostQueryItem item = new PostQueryItem(
                501L, 10L, 1L, "writer", null, "제목", "내용", 3, 9, 0, 2, LocalDateTime.of(2026, 9, 27, 10, 0));
        when(port.findPopularPosts(1, 10)).thenReturn(List.of(item));
        when(port.countAllPosts()).thenReturn(11L);

        PopularPostListResult result = useCase.execute(1, 10);

        assertThat(result).isEqualTo(new PopularPostListResult(1, 10, 11L, List.of(item)));
    }

    @Test
    void 페이지_번호와_크기가_잘못되면_조회하지_않고_VALIDATION_ERROR() {
        for (int[] values : new int[][]{{-1, 20}, {0, 0}, {0, 101}}) {
            assertThatThrownBy(() -> useCase.execute(values[0], values[1]))
                    .isInstanceOf(PostApplicationException.class)
                    .extracting("errorCode").isEqualTo(PostErrorCode.VALIDATION_ERROR);
        }
        verifyNoInteractions(port);
    }
}
