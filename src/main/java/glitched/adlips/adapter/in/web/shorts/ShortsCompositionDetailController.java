package glitched.adlips.adapter.in.web.shorts;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.ShortsCompositionDetailResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.shorts.GetShortsCompositionDetailUseCase;
import glitched.adlips.application.shorts.ShortsCompositionDetailResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shorts")
public class ShortsCompositionDetailController {

    private final GetShortsCompositionDetailUseCase getShortsCompositionDetailUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ShortsCompositionDetailController(
            GetShortsCompositionDetailUseCase getShortsCompositionDetailUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.getShortsCompositionDetailUseCase = getShortsCompositionDetailUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping("/{shortId}/composition-details")
    public ResponseEntity<ApiResponse<ShortsCompositionDetailResponse>> getCompositionDetail(
            @PathVariable Long shortId,
            @RequestHeader("Authorization") String authorization
    ) {
        authenticatedUserResolver.requireUserId(authorization);
        ShortsCompositionDetailResult result = getShortsCompositionDetailUseCase.execute(shortId);
        return ResponseEntity.ok(ApiResponse.success(
                "작곡 상세 정보 조회가 완료되었습니다.", ShortsCompositionDetailResponse.from(result)));
    }
}
