package glitched.adlips.adapter.in.web.community;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import glitched.adlips.adapter.in.web.dto.ApiResponse;
import glitched.adlips.adapter.in.web.dto.GalleryListItemResponse;
import glitched.adlips.adapter.in.web.dto.GallerySubscriptionResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.community.GetGalleriesUseCase;
import glitched.adlips.application.community.GetManageableGalleriesUseCase;
import glitched.adlips.application.community.GetSubscribedGalleriesUseCase;
import glitched.adlips.application.community.SubscribeGalleryUseCase;
import glitched.adlips.application.community.UnsubscribeGalleryUseCase;
import glitched.adlips.application.community.port.out.GalleryQueryItem;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "갤러리", description = "갤러리 API")
@RestController
@RequestMapping({"/api/v1/communities", "/api/v1/galleries"})
public class GalleryController {

    private final GetGalleriesUseCase getGalleriesUseCase;
    private final GetManageableGalleriesUseCase getManageableGalleriesUseCase;
    private final GetSubscribedGalleriesUseCase getSubscribedGalleriesUseCase;
    private final SubscribeGalleryUseCase subscribeGalleryUseCase;
    private final UnsubscribeGalleryUseCase unsubscribeGalleryUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public GalleryController(
            GetGalleriesUseCase getGalleriesUseCase,
            GetManageableGalleriesUseCase getManageableGalleriesUseCase,
            GetSubscribedGalleriesUseCase getSubscribedGalleriesUseCase,
            SubscribeGalleryUseCase subscribeGalleryUseCase,
            UnsubscribeGalleryUseCase unsubscribeGalleryUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.getGalleriesUseCase = getGalleriesUseCase;
        this.getManageableGalleriesUseCase = getManageableGalleriesUseCase;
        this.getSubscribedGalleriesUseCase = getSubscribedGalleriesUseCase;
        this.subscribeGalleryUseCase = subscribeGalleryUseCase;
        this.unsubscribeGalleryUseCase = unsubscribeGalleryUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GalleryListItemResponse>>> getGalleries() {
        List<GalleryQueryItem> items = getGalleriesUseCase.execute();
        return ResponseEntity.ok(ApiResponse.success(
                "갤러리 목록 조회가 완료되었습니다.", toResponses(items)));
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/manageable")
    public ResponseEntity<ApiResponse<List<GalleryListItemResponse>>> getManageableGalleries(
            @RequestHeader("Authorization") String authorization
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        return ResponseEntity.ok(ApiResponse.success(
                "관리 가능한 갤러리 목록 조회가 완료되었습니다.",
                toResponses(getManageableGalleriesUseCase.execute(userId))));
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/subscribed")
    public ResponseEntity<ApiResponse<List<GalleryListItemResponse>>> getSubscribedGalleries(
            @RequestHeader("Authorization") String authorization
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        List<GalleryQueryItem> items = getSubscribedGalleriesUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.success(
                "가입한 갤러리 목록 조회가 완료되었습니다.", toResponses(items)));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{galleryId}/subscriptions")
    public ResponseEntity<ApiResponse<GallerySubscriptionResponse>> subscribe(
            @PathVariable Long galleryId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        subscribeGalleryUseCase.execute(userId, galleryId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "해당 갤러리를 성공적으로 구독하였습니다.", new GallerySubscriptionResponse(galleryId)));
    }

    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{galleryId}/subscriptions")
    public ResponseEntity<ApiResponse<Void>> unsubscribe(
            @PathVariable Long galleryId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        unsubscribeGalleryUseCase.execute(userId, galleryId);
        return ResponseEntity.ok(ApiResponse.success("해당 갤러리의 구독을 취소하였습니다.", null));
    }

    private static List<GalleryListItemResponse> toResponses(List<GalleryQueryItem> items) {
        return items.stream().map(GalleryListItemResponse::from).toList();
    }
}
