package glitched.adlips.application.shorts;

import java.util.List;

public record ShortRecommendationPage(List<Item> items, Page page) {
    public record Item(
            Long shortId,
            String title,
            String albumImageUrl,
            AuthorSummary author,
            RecommendationReason reason
    ) {}

    public record Page(String nextCursor, boolean hasMore, int size) {}
}
