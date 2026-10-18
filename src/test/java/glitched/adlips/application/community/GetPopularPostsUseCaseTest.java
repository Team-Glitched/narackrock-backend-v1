package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.PostQueryItem;
import glitched.adlips.application.community.port.out.PostQueryPort;
import glitched.adlips.application.community.port.out.PostPopularityRankingPort;
import glitched.adlips.application.community.port.out.PostPopularityScore;
import glitched.adlips.application.port.TransactionRunner;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetPopularPostsUseCaseTest {

    final PostQueryPort port = mock(PostQueryPort.class);
    final PostPopularityRankingPort ranking = mock(PostPopularityRankingPort.class);
    final GetPopularPostsUseCase useCase = new GetPopularPostsUseCase(port, ranking, TransactionRunner.direct());

    @Test
    void 전체_갤러리의_인기글과_전체_수를_반환한다() {
        PostQueryItem item = new PostQueryItem(
                501L, 10L, 1L, "writer", null, "제목", "내용", 3, 9, 0, 2, LocalDateTime.of(2026, 9, 27, 10, 0));
        when(port.findPopularityScores()).thenReturn(List.of(new PostPopularityScore(501L, 3, 9)));
        when(ranking.findTop(10)).thenReturn(List.of(501L));
        when(port.findByIds(List.of(501L))).thenReturn(List.of(item));
        when(port.countAllPosts()).thenReturn(11L);

        PopularPostListResult result = useCase.execute(0, 10);

        assertThat(result).isEqualTo(new PopularPostListResult(0, 10, 11L, List.of(item)));
        verify(ranking).seedBaseline(List.of(new PostPopularityScore(501L, 3, 9)));
    }

    @Test
    void 페이지_번호와_크기가_잘못되면_조회하지_않고_VALIDATION_ERROR() {
        for (int[] values : new int[][]{{-1, 20}, {0, 0}, {0, 101}, {Integer.MAX_VALUE, 20}}) {
            assertThatThrownBy(() -> useCase.execute(values[0], values[1]))
                    .isInstanceOf(PostApplicationException.class)
                    .extracting("errorCode").isEqualTo(PostErrorCode.VALIDATION_ERROR);
        }
        verifyNoInteractions(port, ranking);
    }

    @Test
    void 인기_점수_순서로_100위까지만_반환한다() {
        PostQueryItem first = new PostQueryItem(1L, 10L, 1L, "a", null, "첫째", "내용", 3, 5, 0, 0, LocalDateTime.now());
        PostQueryItem second = new PostQueryItem(2L, 10L, 1L, "a", null, "둘째", "내용", 2, 6, 0, 0, LocalDateTime.now());
        when(ranking.findTop(100)).thenReturn(List.of(2L, 1L));
        when(port.findByIds(List.of(2L, 1L))).thenReturn(List.of(first, second));
        when(port.countAllPosts()).thenReturn(120L);

        assertThat(useCase.execute(0, 100)).isEqualTo(new PopularPostListResult(0, 100, 100L, List.of(second, first)));
        assertThat(useCase.execute(1, 100)).isEqualTo(new PopularPostListResult(1, 100, 100L, List.of()));
    }
}
