package glitched.adlips.adapter.in.web.shorts;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.ShortsResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.shorts.GetShortsUseCase;
import glitched.adlips.application.shorts.ShortsPage;
import glitched.adlips.application.shorts.ShortsSource;
import glitched.adlips.domain.shorts.ShortStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shorts")
public class ShortsController {
    private static final int MAX_PAGE_SIZE = 100;

    private final GetShortsUseCase getShortsUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ShortsController(
            GetShortsUseCase getShortsUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.getShortsUseCase = getShortsUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ShortsResponse>> getShorts(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "SHORTS_FEED") ShortsSource source,
            @RequestParam(defaultValue = "COMPLETED") ShortStatus status,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        int clampedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Long currentUserId = authenticatedUserResolver.resolveOptionalUserId(authorization);
        ShortsPage page = getShortsUseCase.get(cursor, source, status, clampedSize, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(
                "숏폼 목록 조회가 완료되었습니다.", ShortsResponse.from(page)));
    }
}
