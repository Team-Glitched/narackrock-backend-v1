package glitched.adlips.application.user;

import java.util.List;

public record UserSearchResult(
        String searchKeyword,
        long totalResultCount,
        List<SearchUser> users
) {
    public record SearchUser(
            Long userId,
            String nickname,
            String profileImageUrl,
            String primaryInstrument,
            String explanation,
            boolean isFollowing
    ) {
    }
}
