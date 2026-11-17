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
import java.time.LocalDateTime;
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
    void mixesSixShuffledPersonalizedAndFourDiscoverySongs() {
        configureCatalog();
        when(follows.findFollowingIds(1L)).thenReturn(List.of(11L, 12L, 13L, 14L));
        when(candidates.findEngagedAuthors(1L)).thenReturn(Map.of(
                11L, ArtistAffinity.SAVED, 15L, ArtistAffinity.SAVED,
                16L, ArtistAffinity.LIKED));
        when(collaborations.findCollaboratorIds(1L)).thenReturn(List.of(17L));
        when(candidates.findForAuthors(eq(1L), anySet(), anyInt())).thenAnswer(call -> {
            Set<Long> authors = call.getArgument(1);
            return visible.values().stream().filter(song -> authors.contains(song.authorId())).toList();
        });

        var page = service().execute(1L, 10, null);

        assertThat(page.items()).hasSize(10);
        Set<Long> affiliatedArtists = Set.of(11L, 12L, 13L, 14L, 15L, 16L, 17L);
        assertThat(page.items()).filteredOn(item -> affiliatedArtists.contains(item.author().userId()))
                .extracting(item -> item.author().userId()).hasSize(6);
        assertThat(page.page().nextCursor()).isNotBlank();

    }

    @Test
    void snapshotKeepsPaginationOrderAndSkipsUnavailableSongs() {
        configureCatalog();
        var service = service();
        var first = service.execute(1L, 4, null);
        var original = service.execute(1L, 6, first.page().nextCursor());
        long nowLiked = original.items().get(0).shortId();
        long nowUnavailable = original.items().get(1).shortId();
        interacted.add(nowLiked);
        visible.remove(nowUnavailable);

        var second = service().execute(1L, 4, first.page().nextCursor());

        assertThat(second.items()).containsExactlyElementsOf(original.items().subList(2, 6));
        assertThatThrownBy(() -> service.execute(2L, 4, first.page().nextCursor()))
                .isInstanceOfSatisfying(GetRecommendedShortsUseCase.CursorException.class,
                        exception -> assertThat(exception.expired()).isTrue());
    }

    @Test
    void guestsReceiveNewShuffledDiscoveryLists() {
        configureCatalog();
        var guest = service().execute(null, 10, null);
        verifyNoInteractions(follows, collaborations);
        assertThat(guest.items()).hasSize(10);
        var orders = LongStream.range(0, 10)
                .mapToObj(attempt -> service().execute(null, 10, null).items())
                .distinct().toList();
        assertThat(orders).hasSizeGreaterThan(1);
    }

    @Test
    void deduplicatesDiscoveryCandidatesAndRejectsInvalidCursors() {
        configureCatalog();
        var only = List.of(visible.get(1L), visible.get(9L), visible.get(10L));
        when(candidates.findForDiscovery(isNull(), anySet(), isNull(), anyInt())).thenReturn(only);
        when(candidates.findForDiscovery(isNull(), anySet(), anyLong(), anyInt())).thenReturn(only);

        var page = service().execute(null, 10, null);

        assertThat(page.items()).extracting(ShortRecommendationPage.Item::shortId).containsExactlyInAnyOrder(1L, 10L, 9L);
        assertThat(page.page().hasMore()).isFalse();
        assertThatThrownBy(() -> service().execute(null, 10, "invalid"))
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
        Map<List<Object>, List<Long>> stored = new HashMap<>();
        org.mockito.Mockito.doAnswer(call -> {
            stored.put(java.util.Arrays.asList(call.getArgument(0), call.getArgument(1)), call.getArgument(2));
            return null;
        }).when(snapshots).save(any(), any(UUID.class), any());
        when(snapshots.find(any(), any(UUID.class))).thenAnswer(call ->
                Optional.ofNullable(stored.get(java.util.Arrays.asList(call.getArgument(0), call.getArgument(1)))));
    }

    private GetRecommendedShortsUseCase service() {
        return new GetRecommendedShortsUseCase(
                candidates, shorts, follows, collaborations, snapshots);
    }

    private ShortsQueryItem song(Long id) {
        return new ShortsQueryItem(id, null, "곡 " + id, null, null, ShortStatus.COMPLETED,
                0, 0, 0, 0, 0, interacted.contains(id), false, false, false, false,
                visible.get(id).authorId(), "작곡가", null, LocalDateTime.MIN);
    }
}
