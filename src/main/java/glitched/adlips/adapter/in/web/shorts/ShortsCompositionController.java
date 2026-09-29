package glitched.adlips.adapter.in.web.shorts;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.ShortsCompositionResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.shorts.ShortsCompositionEntryUseCase;
import glitched.adlips.application.shorts.port.out.ShortsCompositionQueryItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "숏폼 구성", description = "숏폼 구성 API")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/shorts")
public class ShortsCompositionController {

    private final ShortsCompositionEntryUseCase shortsCompositionEntryUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ShortsCompositionController(
            ShortsCompositionEntryUseCase shortsCompositionEntryUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.shortsCompositionEntryUseCase = shortsCompositionEntryUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping("/{shortId}/composition")
    public ResponseEntity<ApiResponse<ShortsCompositionResponse>> getCompositionEntry(
            @PathVariable Long shortId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        ShortsCompositionQueryItem item = shortsCompositionEntryUseCase.execute(shortId, userId);
        return ResponseEntity.ok(ApiResponse.success(
                "작곡 화면 진입 정보 조회가 완료되었습니다.",
                ShortsCompositionResponse.from(item)));
    }
}
