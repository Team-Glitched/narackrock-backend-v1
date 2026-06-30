package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.shorts.AuthorSummary;
import glitched.adlips.application.shorts.ParticipantSummary;
import glitched.adlips.application.shorts.ShortSummary;
import glitched.adlips.application.shorts.ShortsPage;
import java.time.LocalDateTime;
import java.util.List;

public record ShortsResponse(List<ShortItem> items, Page page) {

    public record ShortItem(
            Long shortId,
            Long projectId,
            String title,
            String mediaUrl,
            String albumImageUrl,
            String completionStatus,
            int viewCount,
            int likeCount,
            int dislikeCount,
            long commentCount,
            int contributionCount,
            boolean isLiked,
            boolean isDisliked,
            boolean isBookmarked,
            Author author,
            List<Participant> participants,
            LocalDateTime createdAt
    ) {
        public record Author(Long userId, String nickname, String profileImageUrl) {}
        public record Participant(Long userId, String nickname, String role) {}
    }

    public record Page(Long nextCursor, boolean hasMore, int size) {}

    public static ShortsResponse from(ShortsPage shortsPage) {
        List<ShortItem> items = shortsPage.items().stream().map(s -> new ShortItem(
                s.shortId(), s.projectId(), s.title(), s.mediaUrl(), s.albumImageUrl(),
                s.completionStatus(), s.viewCount(), s.likeCount(), s.dislikeCount(),
                s.commentCount(), s.contributionCount(),
                s.isLiked(), s.isDisliked(), s.isBookmarked(),
                toAuthor(s.author()),
                toParticipants(s.participants()),
                s.createdAt()
        )).toList();
        Page page = new Page(shortsPage.nextCursor(), shortsPage.hasMore(), shortsPage.size());
        return new ShortsResponse(items, page);
    }

    private static ShortItem.Author toAuthor(AuthorSummary a) {
        return new ShortItem.Author(a.userId(), a.nickname(), a.profileImageUrl());
    }

    private static List<ShortItem.Participant> toParticipants(List<ParticipantSummary> ps) {
        return ps.stream()
                .map(p -> new ShortItem.Participant(p.userId(), p.nickname(), p.role()))
                .toList();
    }
}
