package glitched.adlips.application.user.relation;

public record FollowResult(
        Long targetUserId,
        boolean isFollowing,
        int targetFollowerCount,
        int myFollowingCount
) {
}
