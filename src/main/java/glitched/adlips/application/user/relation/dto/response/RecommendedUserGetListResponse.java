package glitched.adlips.application.user.relation.dto.response;

import glitched.adlips.application.user.relation.model.RecommendType;
import java.util.List;

public record RecommendedUserGetListResponse(
        long totalCount,
        boolean fallbackTriggered,
        List<RecommendedUserResponse> recommendedUsers
) {
    public record RecommendedUserResponse(
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
