package glitched.adlips.application.shorts;

import glitched.adlips.application.shorts.port.out.ShortPopularityRankingPort;
import glitched.adlips.application.shorts.port.out.ShortPopularityScore;
import glitched.adlips.application.shorts.port.out.ShortReactionCountCachePort;
import glitched.adlips.application.shorts.port.out.ShortsParticipantQueryItem;
import glitched.adlips.application.shorts.port.out.ShortsQueryItem;
import glitched.adlips.application.shorts.port.out.ShortsQueryPort;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GetHomeShortsUseCase {
    private final ShortsQueryPort queryPort;
    private final ShortReactionCountCachePort reactionCountCache;
    private final ShortPopularityRankingPort rankingPort;

    public GetHomeShortsUseCase(
            ShortsQueryPort queryPort,
            ShortReactionCountCachePort reactionCountCache,
            ShortPopularityRankingPort rankingPort
    ) {
        this.queryPort = queryPort;
        this.reactionCountCache = reactionCountCache;
        this.rankingPort = rankingPort;
    }

    public ShortsPage popular(int size, Long currentUserId) {
        rankingPort.seedBaseline(queryPort.findPopularityScores());
        List<Long> rankedIds = rankingPort.findTop(size);
        if (rankedIds.isEmpty()) return new ShortsPage(List.of(), null, false, size);

        Map<Long, ShortsQueryItem> byId = queryPort.findByIds(rankedIds, currentUserId).stream()
                .collect(Collectors.toMap(ShortsQueryItem::shortId, Function.identity()));
        List<ShortsQueryItem> ordered = rankedIds.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
        return page(ordered, null, false, size);
    }

    public ShortsPage mine(Long userId, Long cursor, int size) {
        List<ShortsQueryItem> fetched = queryPort.findParticipatedByCursor(
                userId, cursor, size + 1, userId);
        boolean hasMore = fetched.size() > size;
        List<ShortsQueryItem> visible = hasMore ? fetched.subList(0, size) : fetched;
        Long nextCursor = visible.isEmpty() ? null : visible.getLast().shortId();
        return page(visible, nextCursor, hasMore, size);
    }

    private ShortsPage page(List<ShortsQueryItem> items, Long nextCursor, boolean hasMore, int size) {
        if (items.isEmpty()) return new ShortsPage(List.of(), nextCursor, hasMore, size);
        List<Long> ids = items.stream().map(ShortsQueryItem::shortId).toList();
        Map<Long, List<ShortsParticipantQueryItem>> participants = queryPort.findParticipants(ids)
                .stream().collect(Collectors.groupingBy(ShortsParticipantQueryItem::shortId));
        Map<Long, ShortReactionCounts> cached = reactionCountCache.findAll(ids);
        List<ShortSummary> summaries = items.stream().map(item -> {
            ShortReactionCounts counts = cached.getOrDefault(
                    item.shortId(), new ShortReactionCounts(item.likeCount(), item.dislikeCount()));
            return new ShortSummary(
                    item.shortId(), item.projectId(), item.title(), item.mediaUrl(), item.albumImageUrl(),
                    item.status().name(), item.viewCount(), counts.likeCount(), counts.dislikeCount(),
                    item.commentCount(), item.contributionCount(), item.liked(), item.disliked(),
                    item.bookmarked(), item.hasPendingContribution(), item.hasApprovedContribution(),
                    new AuthorSummary(item.authorId(), item.authorNickname(), item.authorProfileImageUrl()),
                    participants.getOrDefault(item.shortId(), List.of()).stream()
                            .map(p -> new ParticipantSummary(p.userId(), p.nickname(), p.role())).toList(),
                    item.createdAt());
        }).toList();
        return new ShortsPage(summaries, nextCursor, hasMore, size);
    }
}
