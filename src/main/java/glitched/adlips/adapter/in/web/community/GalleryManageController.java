package glitched.adlips.adapter.in.web.community;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import glitched.adlips.adapter.in.web.dto.ApiResponse;
import glitched.adlips.adapter.in.web.dto.GalleryBanPageResponse;
import glitched.adlips.adapter.in.web.dto.GalleryContentDeleteRequest;
import glitched.adlips.adapter.in.web.dto.GalleryManagerGrantRequest;
import glitched.adlips.adapter.in.web.dto.GalleryManagerResponse;
import glitched.adlips.adapter.in.web.dto.GalleryModerationLogPageResponse;
import glitched.adlips.adapter.in.web.dto.GalleryRoleResponse;
import glitched.adlips.adapter.in.web.dto.GallerySanctionRequest;
import glitched.adlips.adapter.in.web.dto.GallerySanctionResponse;
import glitched.adlips.adapter.in.web.dto.GalleryUpdateRequest;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.community.GetGalleryBansUseCase;
import glitched.adlips.application.community.GetGalleryManagersUseCase;
import glitched.adlips.application.community.GetGalleryModerationLogsUseCase;
import glitched.adlips.application.community.GetMyGalleryRoleUseCase;
import glitched.adlips.application.community.GrantGalleryManagerUseCase;
import glitched.adlips.application.community.LiftGalleryBanUseCase;
import glitched.adlips.application.community.ModeratePostCommentDeletionUseCase;
import glitched.adlips.application.community.ModeratePostDeletionUseCase;
import glitched.adlips.application.community.RevokeGalleryManagerUseCase;
import glitched.adlips.application.community.SanctionGalleryUserUseCase;
import glitched.adlips.application.community.UpdateGalleryUseCase;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "갤러리 관리", description = "갤러리 매니저·관리자용 갤러리 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/galleries/{galleryId}/manage")
public class GalleryManageController {

    private final GetMyGalleryRoleUseCase getMyGalleryRoleUseCase;
    private final UpdateGalleryUseCase updateGalleryUseCase;
    private final GetGalleryModerationLogsUseCase getGalleryModerationLogsUseCase;
    private final SanctionGalleryUserUseCase sanctionGalleryUserUseCase;
    private final LiftGalleryBanUseCase liftGalleryBanUseCase;
    private final GetGalleryBansUseCase getGalleryBansUseCase;
    private final ModeratePostDeletionUseCase moderatePostDeletionUseCase;
    private final ModeratePostCommentDeletionUseCase moderatePostCommentDeletionUseCase;
    private final GetGalleryManagersUseCase getGalleryManagersUseCase;
    private final GrantGalleryManagerUseCase grantGalleryManagerUseCase;
    private final RevokeGalleryManagerUseCase revokeGalleryManagerUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public GalleryManageController(
            GetMyGalleryRoleUseCase getMyGalleryRoleUseCase,
            UpdateGalleryUseCase updateGalleryUseCase,
            GetGalleryModerationLogsUseCase getGalleryModerationLogsUseCase,
            SanctionGalleryUserUseCase sanctionGalleryUserUseCase,
            LiftGalleryBanUseCase liftGalleryBanUseCase,
            GetGalleryBansUseCase getGalleryBansUseCase,
            ModeratePostDeletionUseCase moderatePostDeletionUseCase,
            ModeratePostCommentDeletionUseCase moderatePostCommentDeletionUseCase,
            GetGalleryManagersUseCase getGalleryManagersUseCase,
            GrantGalleryManagerUseCase grantGalleryManagerUseCase,
            RevokeGalleryManagerUseCase revokeGalleryManagerUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.getMyGalleryRoleUseCase = getMyGalleryRoleUseCase;
        this.updateGalleryUseCase = updateGalleryUseCase;
        this.getGalleryModerationLogsUseCase = getGalleryModerationLogsUseCase;
        this.sanctionGalleryUserUseCase = sanctionGalleryUserUseCase;
        this.liftGalleryBanUseCase = liftGalleryBanUseCase;
        this.getGalleryBansUseCase = getGalleryBansUseCase;
        this.moderatePostDeletionUseCase = moderatePostDeletionUseCase;
        this.moderatePostCommentDeletionUseCase = moderatePostCommentDeletionUseCase;
        this.getGalleryManagersUseCase = getGalleryManagersUseCase;
        this.grantGalleryManagerUseCase = grantGalleryManagerUseCase;
        this.revokeGalleryManagerUseCase = revokeGalleryManagerUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<GalleryRoleResponse>> getMyRole(
            @PathVariable Long galleryId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        return ResponseEntity.ok(ApiResponse.success(
                "갤러리 내 역할 조회가 완료되었습니다.",
                GalleryRoleResponse.from(getMyGalleryRoleUseCase.execute(userId, galleryId))));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable Long galleryId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody GalleryUpdateRequest request
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        updateGalleryUseCase.execute(userId, galleryId, request.description());
        return ResponseEntity.ok(ApiResponse.success("갤러리 정보가 수정되었습니다.", null));
    }

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<GalleryModerationLogPageResponse>> getLogs(
            @PathVariable Long galleryId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        return ResponseEntity.ok(ApiResponse.success(
                "갤러리 관리 이력 조회가 완료되었습니다.",
                GalleryModerationLogPageResponse.from(
                        getGalleryModerationLogsUseCase.execute(userId, galleryId, cursor, size))));
    }

    @GetMapping("/bans")
    public ResponseEntity<ApiResponse<GalleryBanPageResponse>> getBans(
            @PathVariable Long galleryId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        return ResponseEntity.ok(ApiResponse.success(
                "갤러리 제재 목록 조회가 완료되었습니다.",
                GalleryBanPageResponse.from(
                        getGalleryBansUseCase.execute(userId, galleryId, activeOnly, cursor, size))));
    }

    @PostMapping("/bans")
    public ResponseEntity<ApiResponse<GallerySanctionResponse>> sanction(
            @PathVariable Long galleryId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody GallerySanctionRequest request
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        Long banId = sanctionGalleryUserUseCase.execute(
                userId, galleryId, request.userId(), request.type(), request.durationDays(), request.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "해당 사용자를 제재하였습니다.", new GallerySanctionResponse(banId)));
    }

    @PatchMapping("/bans/{banId}/lift")
    public ResponseEntity<ApiResponse<Void>> lift(
            @PathVariable Long galleryId,
            @PathVariable Long banId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        liftGalleryBanUseCase.execute(userId, galleryId, banId);
        return ResponseEntity.ok(ApiResponse.success("제재가 해제되었습니다.", null));
    }

    @PatchMapping("/posts/{postId}/delete")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long galleryId,
            @PathVariable Long postId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody GalleryContentDeleteRequest request
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        moderatePostDeletionUseCase.execute(userId, galleryId, postId, request.reason());
        return ResponseEntity.ok(ApiResponse.success("게시글을 삭제하였습니다.", null));
    }

    @PatchMapping("/comments/{commentId}/delete")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long galleryId,
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody GalleryContentDeleteRequest request
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        moderatePostCommentDeletionUseCase.execute(userId, galleryId, commentId, request.reason());
        return ResponseEntity.ok(ApiResponse.success("댓글을 삭제하였습니다.", null));
    }

    @GetMapping("/managers")
    public ResponseEntity<ApiResponse<List<GalleryManagerResponse>>> getManagers(
            @PathVariable Long galleryId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        return ResponseEntity.ok(ApiResponse.success(
                "커뮤니티 어드민 목록 조회가 완료되었습니다.",
                getGalleryManagersUseCase.execute(userId, galleryId).stream()
                        .map(GalleryManagerResponse::from)
                        .toList()));
    }

    @PostMapping("/managers")
    public ResponseEntity<ApiResponse<Void>> grantManager(
            @PathVariable Long galleryId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody GalleryManagerGrantRequest request
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        grantGalleryManagerUseCase.execute(userId, galleryId, request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "커뮤니티 어드민으로 지정하였습니다.", null));
    }

    @DeleteMapping("/managers/{targetUserId}")
    public ResponseEntity<ApiResponse<Void>> revokeManager(
            @PathVariable Long galleryId,
            @PathVariable Long targetUserId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        revokeGalleryManagerUseCase.execute(userId, galleryId, targetUserId);
        return ResponseEntity.ok(ApiResponse.success("커뮤니티 어드민에서 해임하였습니다.", null));
    }
}
