package glitched.adlips.application.shorts;

import glitched.adlips.application.shorts.port.out.ShortRecommendationQueryPort;
import glitched.adlips.application.shorts.port.out.ShortRecommendationQueryPort.Candidate;
import glitched.adlips.application.shorts.port.out.ShortRecommendationQueryPort.CatalogRange;
import glitched.adlips.application.shorts.port.out.ShortRecommendationSnapshotPort;
import glitched.adlips.application.shorts.port.out.ShortsQueryItem;
import glitched.adlips.application.shorts.port.out.ShortsQueryPort;
import glitched.adlips.application.user.relation.port.out.CollaborationUserQueryPort;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GetRecommendedShortsUseCase {
    private static final int PERSONALIZED_LIMIT = 120;
    private static final int RECENT_DISCOVERY_LIMIT = 30;
    private static final int ROTATING_DISCOVERY_LIMIT = 120;
    private static final int GROUP_SIZE = 10;
    private static final Set<Integer> DISCOVERY_SLOTS = Set.of(2, 4, 7, 9);

    private final ShortRecommendationQueryPort candidates;
    private final ShortsQueryPort shorts;
    private final FollowRepositoryPort follows;
    private final CollaborationUserQueryPort collaborations;
    private final ShortRecommendationSnapshotPort snapshots;

    public GetRecommendedShortsUseCase(
            ShortRecommendationQueryPort candidates,
            ShortsQueryPort shorts,
            FollowRepositoryPort follows,
            CollaborationUserQueryPort collaborations,
            ShortRecommendationSnapshotPort snapshots
    ) {
        this.candidates = candidates;
        this.shorts = shorts;
        this.follows = follows;
        this.collaborations = collaborations;
        this.snapshots = snapshots;
    }

    public ShortRecommendationPage execute(Long userId, int size, String continuation) {
        Cursor cursor = continuation == null ? null : Cursor.parse(continuation);
        List<Long> ranking = cursor == null ? shuffleCategories(userId) : snapshots.find(userId, cursor.id())
                .orElseThrow(() -> new CursorException(true));
        if (cursor != null && cursor.offset() >= ranking.size()) {
            throw new CursorException(false);
        }

        Slice slice = slice(ranking, cursor == null ? 0 : cursor.offset(), size, userId);
        String nextCursor = null;
        if (slice.nextOffset() >= 0) {
            UUID snapshotId;
            if (cursor == null) {
                snapshotId = UUID.randomUUID();
                snapshots.save(userId, snapshotId, ranking);
            } else {
                snapshotId = cursor.id();
            }
            nextCursor = snapshotId + ":" + slice.nextOffset();
        }
        return new ShortRecommendationPage(
                slice.items(), new ShortRecommendationPage.Page(nextCursor, nextCursor != null, size));
    }

    private List<Long> shuffleCategories(Long userId) {
        CatalogRange catalog = candidates.findCatalogRange().orElse(null);
        if (catalog == null) return List.of();

        Set<Long> relatedAuthors = relatedAuthors(userId);
        var related = new ArrayList<Long>();
        if (!relatedAuthors.isEmpty()) {
            candidates.findForAuthors(userId, relatedAuthors, PERSONALIZED_LIMIT)
                    .stream().map(Candidate::shortId).distinct().forEach(related::add);
        }

        long pivot = catalog.oldestId() + ThreadLocalRandom.current()
                .nextLong(catalog.newestId() - catalog.oldestId() + 1);
        Set<Long> discoveryIds = new LinkedHashSet<>();
        candidates.findForDiscovery(userId, relatedAuthors, null, RECENT_DISCOVERY_LIMIT)
                .forEach(song -> discoveryIds.add(song.shortId()));
        candidates.findForDiscovery(userId, relatedAuthors, pivot, ROTATING_DISCOVERY_LIMIT)
                .forEach(song -> discoveryIds.add(song.shortId()));
        discoveryIds.removeAll(related);
        var discovery = new ArrayList<>(discoveryIds);

        Collections.shuffle(related);
        Collections.shuffle(discovery);
        var relatedSongs = related.iterator();
        var discoverySongs = discovery.iterator();
        var shuffled = new ArrayList<Long>(related.size() + discovery.size());
        while (relatedSongs.hasNext() || discoverySongs.hasNext()) {
            boolean discoverySlot = DISCOVERY_SLOTS.contains(shuffled.size() % GROUP_SIZE);
            var preferred = discoverySlot ? discoverySongs : relatedSongs;
            var fallback = discoverySlot ? relatedSongs : discoverySongs;
            shuffled.add((preferred.hasNext() ? preferred : fallback).next());
        }
        return List.copyOf(shuffled);
    }

    private Set<Long> relatedAuthors(Long userId) {
        if (userId == null) return Set.of();
        Set<Long> authors = new HashSet<>(follows.findFollowingIds(userId));
        authors.addAll(candidates.findEngagedAuthors(userId).keySet());
        authors.addAll(collaborations.findCollaboratorIds(userId));
        authors.remove(userId);
        return authors;
    }

    private Slice slice(List<Long> ranking, int offset, int size, Long userId) {
        List<ShortRecommendationPage.Item> items = new ArrayList<>(size);
        while (offset < ranking.size()) {
            int end = Math.min(ranking.size(), offset + size + 1 - items.size());
            List<Long> ids = ranking.subList(offset, end);
            Map<Long, ShortsQueryItem> visible = shorts.findByIds(ids, userId).stream()
                    .filter(item -> !item.authorId().equals(userId) && !item.liked() && !item.disliked() && !item.bookmarked())
                    .collect(Collectors.toMap(ShortsQueryItem::shortId, Function.identity()));
            for (; offset < end; offset++) {
                ShortsQueryItem song = visible.get(ranking.get(offset));
                if (song == null) continue;
                if (items.size() == size) return new Slice(List.copyOf(items), offset);
                items.add(new ShortRecommendationPage.Item(
                        song.shortId(), song.title(), song.albumImageUrl(),
                        new AuthorSummary(song.authorId(), song.authorNickname(), song.authorProfileImageUrl())));
            }
        }
        return new Slice(List.copyOf(items), -1);
    }

    private record Slice(List<ShortRecommendationPage.Item> items, int nextOffset) {}

    private record Cursor(UUID id, int offset) {
        private static Cursor parse(String value) {
            if (value.length() < 38 || value.length() > 42 || value.charAt(36) != ':') {
                throw new CursorException(false);
            }
            try {
                UUID id = UUID.fromString(value.substring(0, 36));
                int offset = Integer.parseInt(value.substring(37));
                if (offset < 1) throw new CursorException(false);
                return new Cursor(id, offset);
            } catch (IllegalArgumentException exception) {
                throw new CursorException(false);
            }
        }
    }

    public static class CursorException extends RuntimeException {
        private final boolean expired;

        private CursorException(boolean expired) {
            super(expired ? "추천 목록이 만료됐습니다." : "유효하지 않은 추천 목록 커서입니다.");
            this.expired = expired;
        }

        public boolean expired() {
            return expired;
        }
    }
}
