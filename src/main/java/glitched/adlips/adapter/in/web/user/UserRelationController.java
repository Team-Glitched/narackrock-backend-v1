package glitched.adlips.adapter.in.web.user;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.application.user.relation.FollowCreateUseCase;
import glitched.adlips.application.user.relation.FollowCancelUseCase;
import glitched.adlips.application.user.relation.FollowResult;
import glitched.adlips.application.user.relation.FollowerGetListUseCase;
import glitched.adlips.application.user.relation.FollowingGetListUseCase;
import glitched.adlips.application.user.relation.RecommendationResult;
import glitched.adlips.application.user.relation.RelationType;
import glitched.adlips.application.user.relation.UserCard;
import glitched.adlips.application.user.relation.RecommendedUserGetListUseCase;
import glitched.adlips.application.user.relation.UserSearchUseCase;
import glitched.adlips.application.user.relation.UserSearchResult;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserRelationController {
    private final FollowCreateUseCase followCreateUseCase;
    private final FollowCancelUseCase followCancelUseCase;
    private final FollowingGetListUseCase followingGetListUseCase;
    private final FollowerGetListUseCase followerGetListUseCase;
    private final UserSearchUseCase userSearchUseCase;
    private final RecommendedUserGetListUseCase recommendedUserGetListUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public UserRelationController(
            FollowCreateUseCase followCreateUseCase,
            FollowCancelUseCase followCancelUseCase,
            FollowingGetListUseCase followingGetListUseCase,
            FollowerGetListUseCase followerGetListUseCase,
            UserSearchUseCase userSearchUseCase,
            RecommendedUserGetListUseCase recommendedUserGetListUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.followCreateUseCase = followCreateUseCase;
        this.followCancelUseCase = followCancelUseCase;
        this.followingGetListUseCase = followingGetListUseCase;
        this.followerGetListUseCase = followerGetListUseCase;
        this.userSearchUseCase = userSearchUseCase;
        this.recommendedUserGetListUseCase = recommendedUserGetListUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/{userId}/follow")
    public ApiResponse<FollowResult> follow(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long requesterId = authenticatedUserResolver.requireUserId(authorization);
        return ApiResponse.success(
                "팔로우 상태가 성공적으로 반영되었습니다.",
                followCreateUseCase.execute(requesterId, userId)
        );
    }

    @DeleteMapping("/{userId}/follow")
    public ApiResponse<FollowResult> unfollow(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long requesterId = authenticatedUserResolver.requireUserId(authorization);
        return ApiResponse.success(
                "팔로우가 성공적으로 취소되었습니다.",
                followCancelUseCase.execute(requesterId, userId)
        );
    }

    @GetMapping("/{userId}/relations")
    public ApiResponse<List<UserCard>> getRelations(
            @PathVariable Long userId,
            @RequestParam String type
    ) {
        return ApiResponse.success(
                "팔로우 목록 조회가 완료되었습니다.",
                getRelationList(userId, RelationType.from(type))
        );
    }

    @GetMapping("/search")
    public ApiResponse<UserSearchResult> search(
            @RequestHeader("Authorization") String authorization,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long requesterId = authenticatedUserResolver.requireUserId(authorization);
        return ApiResponse.success(
                "사용자 검색이 완료되었습니다.",
                userSearchUseCase.execute(requesterId, keyword, page, size)
        );
    }

    @GetMapping("/recommendations")
    public ApiResponse<RecommendationResult> recommendations(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long requesterId = authenticatedUserResolver.requireUserId(authorization);
        return ApiResponse.success(
                "인기 및 활발한 활동 유저 기반의 추천 목록 조회가 완료되었습니다.",
                recommendedUserGetListUseCase.execute(requesterId, page, size)
        );
    }

    private List<UserCard> getRelationList(Long userId, RelationType type) {
        return type == RelationType.FOLLOWING
                ? followingGetListUseCase.execute(userId)
                : followerGetListUseCase.execute(userId);
    }
}
