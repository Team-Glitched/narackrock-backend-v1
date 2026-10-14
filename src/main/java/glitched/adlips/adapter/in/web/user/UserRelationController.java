package glitched.adlips.adapter.in.web.user;

import io.swagger.v3.oas.annotations.tags.Tag;
import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.application.user.relation.dto.request.FollowCancelRequest;
import glitched.adlips.application.user.relation.dto.request.FollowerGetListRequest;
import glitched.adlips.application.user.relation.dto.request.FollowingGetListRequest;
import glitched.adlips.application.user.relation.dto.request.RecommendedUserGetListRequest;
import glitched.adlips.application.user.relation.dto.request.UserSearchRequest;
import glitched.adlips.application.user.relation.dto.response.FollowCancelResponse;
import glitched.adlips.application.user.relation.dto.response.RecommendedUserGetListResponse;
import glitched.adlips.application.user.relation.dto.response.UserRelationResponse;
import glitched.adlips.application.user.relation.dto.response.UserSearchResponse;
import glitched.adlips.application.user.relation.model.RelationType;
import glitched.adlips.application.user.relation.usecase.FollowCancelUseCase;
import glitched.adlips.application.user.relation.usecase.FollowRequestUseCase;
import glitched.adlips.application.user.relation.port.out.FollowRequestPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.relation.usecase.FollowerGetListUseCase;
import glitched.adlips.application.user.relation.usecase.FollowingGetListUseCase;
import glitched.adlips.application.user.relation.usecase.RecommendedUserGetListUseCase;
import glitched.adlips.application.user.relation.usecase.UserSearchUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@Tag(name = "사용자 관계", description = "사용자 팔로우 및 관계 API")
@RequestMapping("/api/v1/users")
public class UserRelationController {
    private final FollowRequestUseCase followRequests;
    private final ProfileRepositoryPort profiles;
    private final MediaFileRepositoryPort mediaFiles;
    private final FollowCancelUseCase followCancelUseCase;
    private final FollowingGetListUseCase followingGetListUseCase;
    private final FollowerGetListUseCase followerGetListUseCase;
    private final UserSearchUseCase userSearchUseCase;
    private final RecommendedUserGetListUseCase recommendedUserGetListUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public UserRelationController(
            FollowRequestUseCase followRequests,
            ProfileRepositoryPort profiles,
            MediaFileRepositoryPort mediaFiles,
            FollowCancelUseCase followCancelUseCase,
            FollowingGetListUseCase followingGetListUseCase,
            FollowerGetListUseCase followerGetListUseCase,
            UserSearchUseCase userSearchUseCase,
            RecommendedUserGetListUseCase recommendedUserGetListUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.followRequests = followRequests;
        this.profiles = profiles;
        this.mediaFiles = mediaFiles;
        this.followCancelUseCase = followCancelUseCase;
        this.followingGetListUseCase = followingGetListUseCase;
        this.followerGetListUseCase = followerGetListUseCase;
        this.userSearchUseCase = userSearchUseCase;
        this.recommendedUserGetListUseCase = recommendedUserGetListUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/{userId}/follow")
    public ApiResponse<FollowRequestUseCase.Result> follow(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long requesterId = authenticatedUserResolver.requireUserId(authorization);
        return ApiResponse.success(
                "팔로우 상태가 성공적으로 반영되었습니다.",
                followRequests.follow(requesterId, userId)
        );
    }

    @DeleteMapping("/{userId}/follow")
    public ApiResponse<?> unfollow(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long requesterId = authenticatedUserResolver.requireUserId(authorization);
        if (followRequests.cancel(requesterId, userId)) {
            return ApiResponse.success("팔로우 요청을 취소했습니다.", new FollowRequestUseCase.Result("NONE", null));
        }
        return ApiResponse.success(
                "팔로우가 성공적으로 취소되었습니다.",
                followCancelUseCase.execute(new FollowCancelRequest(requesterId, userId))
        );
    }

    public record FollowRequestCard(Long requestId, Long requesterId, String nickname, String primaryInstrument, String profileImageUrl) {}

    @GetMapping("/me/follow-requests")
    public ApiResponse<List<FollowRequestCard>> incoming(@RequestHeader("Authorization") String authorization) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        return ApiResponse.success("팔로우 요청 목록", followRequests.inbox(userId).stream().map(request -> {
            var profile = profiles.findByUserId(request.requesterId()).orElse(null);
            String imageUrl = profile == null || profile.getProfileImageFileId() == null ? null
                    : mediaFiles.findById(profile.getProfileImageFileId()).map(MediaFile::getFileUrl).orElse(null);
            return new FollowRequestCard(request.id(), request.requesterId(), profile == null ? "알 수 없는 사용자" : profile.getNickname(), profile == null ? null : profile.getPrimaryInstrument(), imageUrl);
        }).toList());
    }

    @GetMapping("/me/follow-requests/outgoing")
    public ApiResponse<List<FollowRequestPort.Request>> outgoing(@RequestHeader("Authorization") String authorization) {
        return ApiResponse.success("보낸 팔로우 요청 목록", followRequests.outgoing(authenticatedUserResolver.requireUserId(authorization)));
    }

    @PostMapping("/me/follow-requests/{requestId}/accept")
    public ApiResponse<Long> accept(@PathVariable Long requestId, @RequestHeader("Authorization") String authorization) {
        followRequests.accept(authenticatedUserResolver.requireUserId(authorization), requestId);
        return ApiResponse.success("팔로우 요청을 승인했습니다.", requestId);
    }

    @PostMapping("/me/follow-requests/{requestId}/reject")
    public ApiResponse<Long> reject(@PathVariable Long requestId, @RequestHeader("Authorization") String authorization) {
        followRequests.reject(authenticatedUserResolver.requireUserId(authorization), requestId);
        return ApiResponse.success("팔로우 요청을 거절했습니다.", requestId);
    }

    @GetMapping("/me/mutual")
    public ApiResponse<List<UserRelationResponse>> mutual(@RequestHeader("Authorization") String authorization) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        Set<Long> following = followingGetListUseCase.execute(new FollowingGetListRequest(userId)).stream().map(UserRelationResponse::userId).collect(Collectors.toSet());
        return ApiResponse.success("맞팔로우 목록", followerGetListUseCase.execute(new FollowerGetListRequest(userId)).stream().filter(user -> following.contains(user.userId())).toList());
    }

    @GetMapping("/{userId}/relations")
    public ApiResponse<List<UserRelationResponse>> getRelations(
            @PathVariable Long userId,
            @RequestParam String type,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        var profile = profiles.findByUserId(userId).orElse(null);
        if (profile != null && profile.isPrivate()) {
            if (authorization == null || !userId.equals(authenticatedUserResolver.requireUserId(authorization))) {
                throw new UserApplicationException(UserErrorCode.PRIVATE_PROFILE, "비공개 프로필의 관계 목록은 볼 수 없습니다.");
            }
        }
        return ApiResponse.success(
                "팔로우 목록 조회가 완료되었습니다.",
                getRelationList(userId, RelationType.from(type))
        );
    }

    @GetMapping("/search")
    public ApiResponse<UserSearchResponse> search(
            @RequestHeader("Authorization") String authorization,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long requesterId = authenticatedUserResolver.requireUserId(authorization);
        return ApiResponse.success(
                "사용자 검색이 완료되었습니다.",
                userSearchUseCase.execute(new UserSearchRequest(requesterId, keyword, page, size))
        );
    }

    @GetMapping("/recommendations")
    public ApiResponse<RecommendedUserGetListResponse> recommendations(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long requesterId = authenticatedUserResolver.requireUserId(authorization);
        return ApiResponse.success(
                "인기 및 활발한 활동 유저 기반의 추천 목록 조회가 완료되었습니다.",
                recommendedUserGetListUseCase.execute(
                        new RecommendedUserGetListRequest(requesterId, page, size)
                )
        );
    }

    private List<UserRelationResponse> getRelationList(Long userId, RelationType type) {
        return type == RelationType.FOLLOWING
                ? followingGetListUseCase.execute(new FollowingGetListRequest(userId))
                : followerGetListUseCase.execute(new FollowerGetListRequest(userId));
    }
}
