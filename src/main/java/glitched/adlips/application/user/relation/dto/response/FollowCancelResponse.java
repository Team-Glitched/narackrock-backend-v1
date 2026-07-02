package glitched.adlips.application.user.relation.dto.response;

public record FollowCancelResponse(
        Long targetUserId,
        boolean isFollowing,
        int targetFollowerCount,
        int myFollowingCount
) {
}
