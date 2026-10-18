package glitched.adlips.adapter.in.web.shorts;

import io.swagger.v3.oas.annotations.tags.Tag;
import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.ShortsResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.shorts.GetShortsUseCase;
import glitched.adlips.application.shorts.GetHomeShortsUseCase;
import glitched.adlips.application.shorts.ShortsPage;
import glitched.adlips.application.shorts.ShortsSource;
import org.springframework.web.bind.annotation.PathVariable;
import glitched.adlips.domain.shorts.ShortStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "숏폼", description = "숏폼 조회 API")
@RequestMapping("/api/v1/shorts")
public class ShortsController {
    private static final int MAX_PAGE_SIZE = 100;

    private final GetShortsUseCase getShortsUseCase;
    private final GetHomeShortsUseCase getHomeShortsUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ShortsController(
            GetShortsUseCase getShortsUseCase,
            GetHomeShortsUseCase getHomeShortsUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.getShortsUseCase = getShortsUseCase;
        this.getHomeShortsUseCase = getHomeShortsUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<ShortsResponse>> getPopularShorts(
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        int clampedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Long currentUserId = authenticatedUserResolver.resolveOptionalUserId(authorization);
        ShortsPage page = getHomeShortsUseCase.popular(clampedSize, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(
                "인기곡 조회가 완료되었습니다.", ShortsResponse.from(page)));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<ShortsResponse>> getMyShorts(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String authorization
    ) {
        int clampedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Long currentUserId = authenticatedUserResolver.requireUserId(authorization);
        ShortsPage page = getHomeShortsUseCase.mine(currentUserId, cursor, clampedSize);
        return ResponseEntity.ok(ApiResponse.success(
                "내 곡 조회가 완료되었습니다.", ShortsResponse.from(page)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ShortsResponse>> getShorts(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "SHORTS_FEED") ShortsSource source,
            @RequestParam(required = false) ShortStatus completionStatus,
            @RequestParam(name = "status", required = false) ShortStatus legacyStatus,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        int clampedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Long currentUserId = authenticatedUserResolver.resolveOptionalUserId(authorization);
        ShortStatus status = resolveStatus(completionStatus, legacyStatus);
        ShortsPage page = getShortsUseCase.get(cursor, source, status, clampedSize, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(
                "숏폼 목록 조회가 완료되었습니다.", ShortsResponse.from(page)));
    }

    @GetMapping("/{shortId}")
    public ResponseEntity<ApiResponse<ShortsResponse.ShortItem>> getShort(
            @PathVariable Long shortId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Long currentUserId = authenticatedUserResolver.resolveOptionalUserId(authorization);
        return ResponseEntity.ok(ApiResponse.success(
                "숏폼 조회가 완료되었습니다.",
                ShortsResponse.fromSummary(getShortsUseCase.getOne(shortId, currentUserId))));
    }

    private ShortStatus resolveStatus(ShortStatus completionStatus, ShortStatus legacyStatus) {
        if (completionStatus != null) {
            return completionStatus;
        }
        return legacyStatus;
    }
}
