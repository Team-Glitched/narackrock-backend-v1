package glitched.adlips.application.shorts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import glitched.adlips.application.shorts.port.out.ShortRecommendationQueryPort;
import glitched.adlips.application.shorts.port.out.ShortRecommendationQueryPort.Candidate;
import glitched.adlips.application.shorts.port.out.ShortRecommendationQueryPort.CatalogRange;
import glitched.adlips.application.shorts.port.out.ShortRecommendationSnapshotPort;
import glitched.adlips.application.shorts.port.out.ShortsQueryItem;
import glitched.adlips.application.shorts.port.out.ShortsQueryPort;
import glitched.adlips.application.user.relation.port.out.CollaborationUserQueryPort;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.domain.shorts.ShortStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class GetRecommendedShortsUseCaseTest {
    private final ShortRecommendationQueryPort candidates = mock(ShortRecommendationQueryPort.class);
    private final ShortsQueryPort shorts = mock(ShortsQueryPort.class);
    private final FollowRepositoryPort follows = mock(FollowRepositoryPort.class);
    private final CollaborationUserQueryPort collaborations = mock(CollaborationUserQueryPort.class);
    private final ShortRecommendationSnapshotPort snapshots = mock(ShortRecommendationSnapshotPort.class);
    private final Map<Long, Candidate> visible = new HashMap<>();
    private final Set<Long> interacted = new HashSet<>();

    @Test
    void mixesSixPersonalizedAndFourDiscoverySongsAndExplainsWhy() {
        configureCatalog();
        when(follows.findFollowingIds(1L)).thenReturn(List.of(11L, 12L, 13L, 14L));
        when(candidates.findEngagedAuthors(1L)).thenReturn(Map.of(
                11L, RecommendationReason.SAVED_ARTIST, 15L, RecommendationReason.SAVED_ARTIST,
                16L, RecommendationReason.LIKED_ARTIST));
        when(collaborations.findCollaboratorIds(1L)).thenReturn(List.of(17L));
        when(candidates.findForAuthors(eq(1L), anySet(), anyInt())).thenAnswer(call -> {
            Set<Long> authors = call.getArgument(1);
            return visible.values().stream().filter(song -> authors.contains(song.authorId())).toList();
        });

        var page = service("2026-11-11").execute(1L, 10, null);

        assertThat(page.items()).hasSize(10)
                .extracting(item -> item.author().userId()).doesNotHaveDuplicates();
        assertThat(page.items()).filteredOn(item -> item.reason() == RecommendationReason.DISCOVERY).hasSize(4);
        assertThat(page.items()).filteredOn(item -> item.author().userId().equals(11L))
                .extracting(ShortRecommendationPage.Item::reason).containsExactly(RecommendationReason.FOLLOWED_ARTIST);
        assertThat(page.items()).extracting(ShortRecommendationPage.Item::reason)
                .contains(RecommendationReason.SAVED_ARTIST, RecommendationReason.LIKED_ARTIST);
        assertThat(page.items()).anyMatch(item -> item.reason() == RecommendationReason.DISCOVERY
                && item.shortId() <= 30L);
        assertThat(page.page().nextCursor()).isNotBlank();
        assertThat(service("2026-11-11").execute(1L, 20, null).items())
                .extracting(ShortRecommendationPage.Item::reason).contains(RecommendationReason.COLLABORATOR);
    }

    @Test
    void snapshotKeepsPaginationOrderAfterTheDayChangesAndSkipsUnavailableSongs() {
        configureCatalog();
        var service = service("2026-11-11");
        var original = service.execute(1L, 20, null);
        var first = service.execute(1L, 4, null);
        long nowLiked = original.items().get(4).shortId();
        long nowUnavailable = original.items().get(5).shortId();
        interacted.add(nowLiked);
        visible.remove(nowUnavailable);

        var second = service("2026-11-12").execute(1L, 4, first.page().nextCursor());

        assertThat(second.items()).containsExactlyElementsOf(original.items().subList(6, 10));
        assertThatThrownBy(() -> service.execute(2L, 4, first.page().nextCursor()))
                .isInstanceOfSatisfying(GetRecommendedShortsUseCase.CursorException.class,
                        exception -> assertThat(exception.expired()).isTrue());
    }

    @Test
    void guestsReceiveDiscoveryAndRotationVariesByDayAndViewer() {
        configureCatalog();
        var guest = service("2026-11-11").execute(null, 10, null);
        verifyNoInteractions(follows, collaborations);
        assertThat(guest.items()).hasSize(10)
                .allMatch(item -> item.reason() == RecommendationReason.DISCOVERY);
        assertThat(guest.items()).extracting(item -> item.author().userId()).doesNotHaveDuplicates();
        assertThat(service("2026-11-12").execute(null, 10, null).items()).isNotEqualTo(guest.items());
        assertThat(service("2026-11-11").execute(2L, 10, null).items()).isNotEqualTo(guest.items());

        var exposed = LongStream.range(100L, 130L).boxed()
                .flatMap(userId -> service("2026-11-11").execute(userId, 10, null).items().stream())
                .map(ShortRecommendationPage.Item::shortId).distinct().toList();
        assertThat(exposed).hasSizeGreaterThan(20);
    }

    @Test
    void repeatsAnArtistOnlyAfterUsingOtherAvailableArtistsAndRejectsInvalidCursors() {
        configureCatalog();
        var only = List.of(visible.get(1L), visible.get(9L), visible.get(10L));
        when(candidates.findForDiscovery(isNull(), anySet(), isNull(), anyInt())).thenReturn(only);
        when(candidates.findForDiscovery(isNull(), anySet(), anyLong(), anyInt())).thenReturn(only);

        var page = service("2026-11-11").execute(null, 10, null);

        assertThat(page.items()).extracting(ShortRecommendationPage.Item::shortId).containsExactly(1L, 10L, 9L);
        assertThat(page.page().hasMore()).isFalse();
        assertThatThrownBy(() -> service("2026-11-11").execute(null, 10, "invalid"))
                .isInstanceOfSatisfying(GetRecommendedShortsUseCase.CursorException.class,
                        exception -> assertThat(exception.expired()).isFalse());
    }

    private void configureCatalog() {
        for (long id = 1; id <= 50; id++) {
            long author = id == 1 || id == 9 ? 51 : id;
            visible.put(id, new Candidate(id, author));
        }
        when(candidates.findCatalogRange()).thenReturn(Optional.of(new CatalogRange(1L, 50L)));
        when(candidates.findForDiscovery(any(), anySet(), any(), anyInt())).thenAnswer(call -> {
            Set<Long> excluded = call.getArgument(1);
            Long pivot = call.getArgument(2);
            if (pivot == null) {
                return visible.values().stream().filter(item -> !excluded.contains(item.authorId()))
                        .sorted((left, right) -> Long.compare(right.shortId(), left.shortId())).limit(30).toList();
            }
            var ordered = new ArrayList<Candidate>();
            for (long step = 0; step < 50; step++) {
                long id = Math.floorMod(pivot - step - 1, 50) + 1;
                Candidate item = visible.get(id);
                if (item != null && !excluded.contains(item.authorId())) ordered.add(item);
            }
            return ordered;
        });
        when(shorts.findByIds(any(), any())).thenAnswer(call -> {
            List<Long> ids = call.getArgument(0);
            return ids.stream().filter(visible::containsKey).map(this::song).toList();
        });
        Map<List<Object>, List<ShortRecommendationSnapshotPort.Entry>> stored = new HashMap<>();
        org.mockito.Mockito.doAnswer(call -> {
            stored.put(java.util.Arrays.asList(call.getArgument(0), call.getArgument(1)), call.getArgument(2));
            return null;
        }).when(snapshots).save(any(), any(UUID.class), any());
        when(snapshots.find(any(), any(UUID.class))).thenAnswer(call ->
                Optional.ofNullable(stored.get(java.util.Arrays.asList(call.getArgument(0), call.getArgument(1)))));
    }

    private GetRecommendedShortsUseCase service(String date) {
        return new GetRecommendedShortsUseCase(
                candidates, shorts, follows, collaborations, snapshots,
                Clock.fixed(Instant.parse(date + "T12:00:00Z"), ZoneOffset.UTC));
    }

    private ShortsQueryItem song(Long id) {
        return new ShortsQueryItem(id, null, "곡 " + id, null, null, ShortStatus.COMPLETED,
                0, 0, 0, 0, 0, interacted.contains(id), false, false, false, false,
                visible.get(id).authorId(), "작곡가", null, LocalDateTime.MIN);
    }
}
