package glitched.adlips.application.user.profile;

import java.util.List;

public record ProfileView(
        Long userId,
        String nickname,
        String profileImageUrl,
        String explanation,
        String primaryInstrument,
        Relations relations,
        Activities activities
) {
    public record Relations(int followerCount, int followingCount, boolean isFollowing) {
    }

    public record Activities(
            int postCount,
            List<PostActivity> posts,
            List<ShortActivity> participatedShorts,
            List<PinnedShortActivity> pinnedShorts
    ) {
        public static Activities empty() {
            return new Activities(0, List.of(), List.of(), List.of());
        }
    }

    public record PostActivity(Long postId, String title, String thumbnailUrl) {
    }

    public record ShortActivity(Long shortId, String title, String albumImageUrl) {
    }

    public record PinnedShortActivity(
            Long shortId,
            String title,
            String albumImageUrl,
            String mediaUrl,
            int displayOrder
    ) {
    }
}
