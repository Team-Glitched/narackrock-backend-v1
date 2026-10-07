package glitched.adlips.adapter.in.web.community;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import glitched.adlips.adapter.in.web.dto.ApiResponse;
import glitched.adlips.adapter.in.web.dto.GalleryModerationLogPageResponse;
import glitched.adlips.adapter.in.web.dto.GalleryRoleResponse;
import glitched.adlips.adapter.in.web.dto.GalleryUpdateRequest;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.community.GetGalleryModerationLogsUseCase;
import glitched.adlips.application.community.GetMyGalleryRoleUseCase;
import glitched.adlips.application.community.UpdateGalleryUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public GalleryManageController(
            GetMyGalleryRoleUseCase getMyGalleryRoleUseCase,
            UpdateGalleryUseCase updateGalleryUseCase,
            GetGalleryModerationLogsUseCase getGalleryModerationLogsUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.getMyGalleryRoleUseCase = getMyGalleryRoleUseCase;
        this.updateGalleryUseCase = updateGalleryUseCase;
        this.getGalleryModerationLogsUseCase = getGalleryModerationLogsUseCase;
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
}
