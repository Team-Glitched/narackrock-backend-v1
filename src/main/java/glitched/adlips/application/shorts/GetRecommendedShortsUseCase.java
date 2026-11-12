package glitched.adlips.application.shorts;

import glitched.adlips.application.shorts.port.out.ShortRecommendationQueryPort;
import glitched.adlips.application.shorts.port.out.ShortRecommendationQueryPort.Candidate;
import glitched.adlips.application.shorts.port.out.ShortRecommendationQueryPort.CatalogRange;
import glitched.adlips.application.shorts.port.out.ShortRecommendationSnapshotPort;
import glitched.adlips.application.shorts.port.out.ShortsQueryItem;
import glitched.adlips.application.shorts.port.out.ShortsQueryPort;
import glitched.adlips.application.user.relation.port.out.CollaborationUserQueryPort;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.UUID;
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
    private final Clock clock;

    public GetRecommendedShortsUseCase(
            ShortRecommendationQueryPort candidates,
            ShortsQueryPort shorts,
            FollowRepositoryPort follows,
            CollaborationUserQueryPort collaborations,
            ShortRecommendationSnapshotPort snapshots,
            Clock clock
    ) {
        this.candidates = candidates;
        this.shorts = shorts;
        this.follows = follows;
        this.collaborations = collaborations;
        this.snapshots = snapshots;
        this.clock = clock;
    }

    public ShortRecommendationPage execute(Long userId, int size, String continuation) {
        Cursor cursor = continuation == null ? null : Cursor.parse(continuation);
        List<Long> ranking = cursor == null ? rank(userId) : snapshots.find(userId, cursor.id())
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

    private List<Long> rank(Long userId) {
        CatalogRange catalog = candidates.findCatalogRange().orElse(null);
        if (catalog == null) return List.of();

        Map<Long, ArtistAffinity> affinity = artistAffinity(userId);
        List<Candidate> personalized = affinity.isEmpty() ? List.of()
                : candidates.findForAuthors(userId, affinity.keySet(), PERSONALIZED_LIMIT);
        List<Candidate> recent = candidates.findForDiscovery(userId, affinity.keySet(), null, RECENT_DISCOVERY_LIMIT);
        long seed = LocalDate.now(clock).toEpochDay() ^ Long.rotateLeft(userId == null ? 0 : userId, 32);
        long pivot = catalog.oldestId() + new SplittableRandom(seed)
                .nextLong(catalog.newestId() - catalog.oldestId() + 1);
        List<Candidate> rotated = candidates.findForDiscovery(userId, affinity.keySet(), pivot, ROTATING_DISCOVERY_LIMIT);

        var personalPool = new ArrayList<>(personalized.stream().sorted(
                Comparator.comparing((Candidate candidate) -> affinity.get(candidate.authorId()))
                        .thenComparing(Candidate::shortId, Comparator.reverseOrder())
        ).toList());
        Set<Long> personalizedIds = personalPool.stream().map(Candidate::shortId).collect(Collectors.toSet());
        var discoveryPool = discoveryPool(recent, rotated, personalizedIds);
        List<Long> ranking = new ArrayList<>(personalPool.size() + discoveryPool.size());
        Set<Long> authors = new HashSet<>();
        Long previousAuthor = null;
        while (!personalPool.isEmpty() || !discoveryPool.isEmpty()) {
            int slot = ranking.size() % GROUP_SIZE;
            if (slot == 0) authors.clear();
            boolean discover = DISCOVERY_SLOTS.contains(slot);
            List<Candidate> preferred = discover ? discoveryPool : personalPool;
            List<Candidate> alternate = discover ? personalPool : discoveryPool;
            Candidate next = take(preferred, alternate, authors, previousAuthor);
            authors.add(next.authorId());
            previousAuthor = next.authorId();
            ranking.add(next.shortId());
        }
        return List.copyOf(ranking);
    }

    private Map<Long, ArtistAffinity> artistAffinity(Long userId) {
        if (userId == null) return Map.of();
        Map<Long, ArtistAffinity> affinity = new HashMap<>();
        follows.findFollowingIds(userId).forEach(id -> affinity.put(id, ArtistAffinity.FOLLOWED));
        candidates.findEngagedAuthors(userId).forEach(affinity::putIfAbsent);
        collaborations.findCollaboratorIds(userId).forEach(id -> affinity.putIfAbsent(id, ArtistAffinity.COLLABORATOR));
        affinity.remove(userId);
        return affinity;
    }

    private static List<Candidate> discoveryPool(
            List<Candidate> recent, List<Candidate> rotated, Set<Long> personalizedIds
    ) {
        Map<Long, Candidate> merged = new LinkedHashMap<>();
        for (int index = 0; index < Math.max(rotated.size(), recent.size()); index++) {
            if (index < rotated.size()) {
                Candidate item = rotated.get(index);
                merged.putIfAbsent(item.shortId(), item);
            }
            if (index < recent.size()) {
                Candidate item = recent.get(index);
                merged.putIfAbsent(item.shortId(), item);
            }
        }
        merged.keySet().removeAll(personalizedIds);
        return new ArrayList<>(merged.values());
    }

    private static Candidate take(
            List<Candidate> preferred, List<Candidate> alternate, Set<Long> authors, Long previousAuthor
    ) {
        Candidate next = takeDifferentAuthor(preferred, authors, previousAuthor);
        if (next == null) next = takeDifferentAuthor(alternate, authors, previousAuthor);
        if (next == null) next = takeDifferentAuthor(preferred, Set.of(), previousAuthor);
        if (next == null) next = takeDifferentAuthor(alternate, Set.of(), previousAuthor);
        if (next != null) return next;
        return (preferred.isEmpty() ? alternate : preferred).removeFirst();
    }

    private static Candidate takeDifferentAuthor(List<Candidate> pool, Set<Long> authors, Long previousAuthor) {
        for (int index = 0; index < pool.size(); index++) {
            Long author = pool.get(index).authorId();
            if (!author.equals(previousAuthor) && !authors.contains(author)) return pool.remove(index);
        }
        return null;
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
