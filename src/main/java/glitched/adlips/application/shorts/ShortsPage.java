package glitched.adlips.application.shorts;

import java.util.List;

public record ShortsPage(List<ShortSummary> items, Long nextCursor, boolean hasMore, int size) {}
