package glitched.adlips.adapter.in.web.community;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import glitched.adlips.adapter.in.web.dto.ApiResponse;
import glitched.adlips.adapter.in.web.dto.GalleryListItemResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.community.GetGalleriesUseCase;
import glitched.adlips.application.community.GetSubscribedGalleriesUseCase;
import glitched.adlips.application.community.port.out.GalleryQueryItem;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "갤러리", description = "갤러리 API")
@RestController
@RequestMapping("/api/v1/galleries")
public class GalleryController {

    private final GetGalleriesUseCase getGalleriesUseCase;
    private final GetSubscribedGalleriesUseCase getSubscribedGalleriesUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public GalleryController(
            GetGalleriesUseCase getGalleriesUseCase,
            GetSubscribedGalleriesUseCase getSubscribedGalleriesUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.getGalleriesUseCase = getGalleriesUseCase;
        this.getSubscribedGalleriesUseCase = getSubscribedGalleriesUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GalleryListItemResponse>>> getGalleries() {
        List<GalleryQueryItem> items = getGalleriesUseCase.execute();
        return ResponseEntity.ok(ApiResponse.success(
                "갤러리 목록 조회가 완료되었습니다.", toResponses(items)));
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

    private static List<GalleryListItemResponse> toResponses(List<GalleryQueryItem> items) {
        return items.stream().map(GalleryListItemResponse::from).toList();
    }
}
