package glitched.adlips.application.user.relation.dto.response;

import java.util.List;

public record UserSearchResponse(
        String searchKeyword,
        long totalResultCount,
        List<SearchUserResponse> users
) {
    public record SearchUserResponse(
            Long userId,
            String nickname,
            String profileImageUrl,
            String primaryInstrument,
            String explanation,
            boolean isFollowing
    ) {
    }
}
