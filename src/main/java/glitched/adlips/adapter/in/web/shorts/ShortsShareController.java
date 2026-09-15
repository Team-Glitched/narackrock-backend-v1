package glitched.adlips.adapter.in.web.shorts;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.ShortsShareResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.shorts.GetShortsShareUseCase;
import glitched.adlips.application.shorts.ShortsShareResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "숏폼 공유", description = "숏폼 공유 API")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/shorts")
public class ShortsShareController {

    private final GetShortsShareUseCase getShortsShareUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ShortsShareController(
            GetShortsShareUseCase getShortsShareUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.getShortsShareUseCase = getShortsShareUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/{shortId}/share")
    public ResponseEntity<ApiResponse<ShortsShareResponse>> getShareLink(
            @PathVariable Long shortId,
            @RequestHeader("Authorization") String authorization
    ) {
        authenticatedUserResolver.requireUserId(authorization);
        ShortsShareResult result = getShortsShareUseCase.execute(shortId);
        return ResponseEntity.ok(ApiResponse.success(
                "공유 링크 조회가 완료되었습니다.", ShortsShareResponse.from(result)));
    }
}
