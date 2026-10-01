package glitched.adlips.application.shorts;

import glitched.adlips.application.shorts.port.out.ShortPopularityRankingPort;
import glitched.adlips.application.shorts.port.out.ShortPopularityScore;
import glitched.adlips.application.shorts.port.out.ShortReactionCountCachePort;
import glitched.adlips.application.shorts.port.out.ShortsQueryItem;
import glitched.adlips.application.shorts.port.out.ShortsQueryPort;
import glitched.adlips.domain.shorts.ShortStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GetHomeShortsUseCaseTest {
    private final ShortsQueryPort queryPort = mock(ShortsQueryPort.class);
    private final ShortReactionCountCachePort reactionCache = mock(ShortReactionCountCachePort.class);
    private final ShortPopularityRankingPort rankingPort = mock(ShortPopularityRankingPort.class);
    private final GetHomeShortsUseCase useCase = new GetHomeShortsUseCase(
            queryPort, reactionCache, rankingPort);

    @Test
    void 인기곡은_가중치_순위의_순서를_유지한다() {
        List<ShortPopularityScore> scores = List.of(
                new ShortPopularityScore(1L, 10, 1),
                new ShortPopularityScore(2L, 1, 5));
        when(queryPort.findPopularityScores()).thenReturn(scores);
        when(rankingPort.findTop(10)).thenReturn(List.of(2L, 1L));
        when(queryPort.findByIds(List.of(2L, 1L), 7L)).thenReturn(List.of(item(1L), item(2L)));
        when(queryPort.findParticipants(List.of(2L, 1L))).thenReturn(List.of());
        when(reactionCache.findAll(List.of(2L, 1L))).thenReturn(Map.of());

        ShortsPage result = useCase.popular(10, 7L);

        verify(rankingPort).seedBaseline(scores);
        assertThat(result.items()).extracting(ShortSummary::shortId).containsExactly(2L, 1L);
    }

    @Test
    void 내_곡은_참여곡만_커서로_페이지_처리한다() {
        when(queryPort.findParticipatedByCursor(7L, null, 3, 7L))
                .thenReturn(List.of(item(9L), item(8L), item(7L)));
        when(queryPort.findParticipants(List.of(9L, 8L))).thenReturn(List.of());
        when(reactionCache.findAll(List.of(9L, 8L))).thenReturn(Map.of());

        ShortsPage result = useCase.mine(7L, null, 2);

        assertThat(result.items()).extracting(ShortSummary::shortId).containsExactly(9L, 8L);
        assertThat(result.hasMore()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(8L);
    }

    private ShortsQueryItem item(long id) {
        return new ShortsQueryItem(
                id, id, "곡 " + id, "https://cdn/" + id, null, ShortStatus.COMPLETED,
                10, 2, 0, 0, 0, false, false, false, false, false,
                100L, "작곡가", null, LocalDateTime.of(2026, 10, 1, 12, 0));
    }
}
