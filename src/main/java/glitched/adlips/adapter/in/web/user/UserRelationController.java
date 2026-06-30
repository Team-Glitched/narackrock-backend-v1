package glitched.adlips.adapter.in.web.user;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.application.user.relation.FollowResult;
import glitched.adlips.application.user.relation.FollowService;
import glitched.adlips.application.user.relation.RecommendationResult;
import glitched.adlips.application.user.relation.RelationType;
import glitched.adlips.application.user.relation.UserCard;
import glitched.adlips.application.user.relation.UserDiscoveryService;
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
    private final FollowService followService;
    private final UserDiscoveryService discoveryService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public UserRelationController(
            FollowService followService,
            UserDiscoveryService discoveryService,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.followService = followService;
        this.discoveryService = discoveryService;
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
                followService.follow(requesterId, userId)
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
                followService.unfollow(requesterId, userId)
        );
    }

    @GetMapping("/{userId}/relations")
    public ApiResponse<List<UserCard>> getRelations(
            @PathVariable Long userId,
            @RequestParam String type
    ) {
        return ApiResponse.success(
                "팔로우 목록 조회가 완료되었습니다.",
                followService.getRelations(userId, RelationType.from(type))
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
                discoveryService.search(requesterId, keyword, page, size)
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
                discoveryService.recommend(requesterId, page, size)
        );
    }
}
