package glitched.adlips.application.user.relation.dto.response;

public record FollowCreateResponse(
        Long targetUserId,
        boolean isFollowing,
        int targetFollowerCount,
        int myFollowingCount
) {
}
