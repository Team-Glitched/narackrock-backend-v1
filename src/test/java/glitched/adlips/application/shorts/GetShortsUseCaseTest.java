package glitched.adlips.application.shorts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import glitched.adlips.application.shorts.port.out.ShortsParticipantQueryItem;
import glitched.adlips.application.shorts.port.out.ShortsQueryItem;
import glitched.adlips.application.shorts.port.out.ShortsQueryPort;
import glitched.adlips.application.shorts.port.out.ShortReactionCountCachePort;
import glitched.adlips.domain.shorts.ShortStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetShortsUseCaseTest {

    @Mock
    private ShortsQueryPort shortsQueryPort;

    @Mock
    private ShortReactionCountCachePort reactionCountCache;

    private GetShortsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetShortsUseCase(shortsQueryPort, reactionCountCache);
    }

    @Test
    void returnsOnlyRequestedSizeAndBuildsNextCursor() {
        given(shortsQueryPort.findByCursor(null, ShortsSource.SHORTS_FEED, ShortStatus.COMPLETED, 3, null))
                .willReturn(List.of(item(3L), item(2L), item(1L)));
        given(shortsQueryPort.findParticipants(List.of(3L, 2L))).willReturn(List.of());

        ShortsPage page = useCase.get(null, ShortsSource.SHORTS_FEED, ShortStatus.COMPLETED, 2, null);

        assertThat(page.items()).extracting(ShortSummary::shortId).containsExactly(3L, 2L);
        assertThat(page.nextCursor()).isEqualTo(2L);
        assertThat(page.hasMore()).isTrue();
        assertThat(page.size()).isEqualTo(2);
    }

    @Test
    void mapsMediaAuthorInteractionAndParticipantsFromQueryPort() {
        ShortsQueryItem item = new ShortsQueryItem(
                12L, 8L, "밤하늘 위 멜로디",
                "https://cdn.example.com/short.mp4",
                "https://cdn.example.com/album.png",
                ShortStatus.COMPLETED,
                1200, 128, 4, 23, 5,
                true, false, true,
                123L, "guitar_moon", "https://cdn.example.com/profile.png",
                LocalDateTime.of(2026, 6, 25, 12, 3)
        );
        given(shortsQueryPort.findByCursor(null, ShortsSource.SHORTS_FEED, ShortStatus.COMPLETED, 21, 123L))
                .willReturn(List.of(item));
        given(shortsQueryPort.findParticipants(List.of(12L))).willReturn(List.of(
                new ShortsParticipantQueryItem(12L, 456L, "beat_sky", "DRUM")
        ));

        ShortSummary result = useCase.get(null, ShortsSource.SHORTS_FEED, ShortStatus.COMPLETED, 20, 123L)
                .items().getFirst();

        assertThat(result.mediaUrl()).isEqualTo("https://cdn.example.com/short.mp4");
        assertThat(result.albumImageUrl()).isEqualTo("https://cdn.example.com/album.png");
        assertThat(result.commentCount()).isEqualTo(23);
        assertThat(result.contributionCount()).isEqualTo(5);
        assertThat(result.isLiked()).isTrue();
        assertThat(result.isDisliked()).isFalse();
        assertThat(result.isBookmarked()).isTrue();
        assertThat(result.author().profileImageUrl()).isEqualTo("https://cdn.example.com/profile.png");
        assertThat(result.participants()).containsExactly(
                new ParticipantSummary(456L, "beat_sky", "DRUM")
        );
        verify(reactionCountCache).put(12L, new ShortReactionCounts(128, 4));
    }

    @Test
    void usesRedisReactionCountsWhenCached() {
        ShortsQueryItem item = item(12L);
        given(shortsQueryPort.findByCursor(
                null, ShortsSource.SHORTS_FEED, ShortStatus.COMPLETED, 21, null))
                .willReturn(List.of(item));
        given(shortsQueryPort.findParticipants(List.of(12L))).willReturn(List.of());
        given(reactionCountCache.findAll(List.of(12L)))
                .willReturn(Map.of(12L, new ShortReactionCounts(999, 8)));

        ShortSummary result = useCase.get(
                null, ShortsSource.SHORTS_FEED, ShortStatus.COMPLETED, 20, null)
                .items().getFirst();

        assertThat(result.likeCount()).isEqualTo(999);
        assertThat(result.dislikeCount()).isEqualTo(8);
    }

    @Test
    void doesNotLoadParticipantsWhenPageIsEmpty() {
        given(shortsQueryPort.findByCursor(10L, ShortsSource.SHORTS_FEED, ShortStatus.COMPLETED, 21, null))
                .willReturn(List.of());

        ShortsPage page = useCase.get(10L, ShortsSource.SHORTS_FEED, ShortStatus.COMPLETED, 20, null);

        assertThat(page.items()).isEmpty();
        assertThat(page.nextCursor()).isNull();
        assertThat(page.hasMore()).isFalse();
        verify(shortsQueryPort).findByCursor(10L, ShortsSource.SHORTS_FEED, ShortStatus.COMPLETED, 21, null);
    }

    private ShortsQueryItem item(Long id) {
        return new ShortsQueryItem(
                id, null, "제목 " + id,
                "https://cdn.example.com/" + id + ".mp4", null,
                ShortStatus.COMPLETED,
                0, 0, 0, 0, 0,
                false, false, false,
                100L + id, "user" + id, null,
                LocalDateTime.of(2026, 6, 25, 12, 3)
        );
    }
}
