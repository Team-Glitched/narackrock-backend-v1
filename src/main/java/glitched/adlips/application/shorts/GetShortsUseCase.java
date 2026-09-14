package glitched.adlips.application.shorts;

import glitched.adlips.application.shorts.port.out.ShortsParticipantQueryItem;
import glitched.adlips.application.shorts.port.out.ShortsQueryItem;
import glitched.adlips.application.shorts.port.out.ShortsQueryPort;
import glitched.adlips.application.shorts.port.out.ShortReactionCountCachePort;
import glitched.adlips.domain.shorts.ShortStatus;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GetShortsUseCase {
    private final ShortsQueryPort shortsQueryPort;
    private final ShortReactionCountCachePort reactionCountCache;

    public GetShortsUseCase(
            ShortsQueryPort shortsQueryPort,
            ShortReactionCountCachePort reactionCountCache
    ) {
        this.shortsQueryPort = shortsQueryPort;
        this.reactionCountCache = reactionCountCache;
    }

    public ShortsPage get(
            Long cursor,
            ShortsSource source,
            ShortStatus status,
            int size,
            Long currentUserId
    ) {
        List<ShortsQueryItem> fetched = shortsQueryPort.findByCursor(
                cursor, source, status, size + 1, currentUserId);
        boolean hasMore = fetched.size() > size;
        List<ShortsQueryItem> visibleItems = hasMore ? fetched.subList(0, size) : fetched;

        if (visibleItems.isEmpty()) {
            return new ShortsPage(List.of(), null, false, size);
        }

        List<Long> shortIds = visibleItems.stream().map(ShortsQueryItem::shortId).toList();
        Map<Long, List<ShortsParticipantQueryItem>> participantsByShortId = shortsQueryPort
                .findParticipants(shortIds)
                .stream()
                .collect(Collectors.groupingBy(ShortsParticipantQueryItem::shortId));
        Map<Long, ShortReactionCounts> cachedCounts = reactionCountCache.findAll(shortIds);

        List<ShortSummary> items = visibleItems.stream()
                .map(item -> {
                    ShortReactionCounts counts = cachedCounts.get(item.shortId());
                    if (counts == null) {
                        counts = new ShortReactionCounts(item.likeCount(), item.dislikeCount());
                        reactionCountCache.put(item.shortId(), counts);
                    }
                    return toSummary(
                            item,
                            participantsByShortId.getOrDefault(item.shortId(), List.of()),
                            counts);
                })
                .toList();
        return new ShortsPage(items, items.getLast().shortId(), hasMore, size);
    }

    private ShortSummary toSummary(
            ShortsQueryItem item,
            List<ShortsParticipantQueryItem> participants,
            ShortReactionCounts reactionCounts
    ) {
        return new ShortSummary(
                item.shortId(),
                item.projectId(),
                item.title(),
                item.mediaUrl(),
                item.albumImageUrl(),
                item.status().name(),
                item.viewCount(),
                reactionCounts.likeCount(),
                reactionCounts.dislikeCount(),
                item.commentCount(),
                item.contributionCount(),
                item.liked(),
                item.disliked(),
                item.bookmarked(),
                new AuthorSummary(
                        item.authorId(), item.authorNickname(), item.authorProfileImageUrl()),
                participants.stream()
                        .map(participant -> new ParticipantSummary(
                                participant.userId(), participant.nickname(), participant.role()))
                        .toList(),
                item.createdAt()
        );
    }
}
