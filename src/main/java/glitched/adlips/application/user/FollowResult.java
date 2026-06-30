package glitched.adlips.application.user;

public record FollowResult(
        Long targetUserId,
        boolean isFollowing,
        int targetFollowerCount,
        int myFollowingCount
) {
}
