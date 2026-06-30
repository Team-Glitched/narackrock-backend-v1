package glitched.adlips.application.user.relation;

import java.util.List;

public record RecommendationResult(
        long totalCount,
        boolean fallbackTriggered,
        List<RecommendedUser> recommendedUsers
) {
    public record RecommendedUser(
            Long userId,
            String nickname,
            String profileImageUrl,
            String primaryInstrument,
            RecommendType recommendType,
            String explanation,
            boolean isFollowing
    ) {
    }
}
