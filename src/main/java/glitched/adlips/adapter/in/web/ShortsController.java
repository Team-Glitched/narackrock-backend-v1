package glitched.adlips.adapter.in.web;

import glitched.adlips.adapter.in.web.dto.ApiResponse;
import glitched.adlips.adapter.in.web.dto.ShortsResponse;
import glitched.adlips.application.shorts.GetShortsUseCase;
import glitched.adlips.application.shorts.ShortsPage;
import glitched.adlips.domain.shorts.ShortStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shorts")
public class ShortsController {

    private final GetShortsUseCase getShortsUseCase;

    public ShortsController(GetShortsUseCase getShortsUseCase) {
        this.getShortsUseCase = getShortsUseCase;
    }

    private static final int MAX_PAGE_SIZE = 100;

    @GetMapping
    public ResponseEntity<ApiResponse<ShortsResponse>> getShorts(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "COMPLETED") ShortStatus status
    ) {
        int clampedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Long currentUserId = resolveCurrentUserId();
        ShortsPage page = getShortsUseCase.get(cursor, status, clampedSize, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("숏폼 목록 조회가 완료되었습니다.", ShortsResponse.from(page)));
    }

    private Long resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        return null;
    }
}
