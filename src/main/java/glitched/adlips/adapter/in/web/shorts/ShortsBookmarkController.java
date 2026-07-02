package glitched.adlips.adapter.in.web.shorts;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.ShortBookmarkResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.shorts.ShortBookmarkResult;
import glitched.adlips.application.shorts.ToggleShortBookmarkUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shorts")
public class ShortsBookmarkController {

    private final ToggleShortBookmarkUseCase toggleShortBookmarkUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ShortsBookmarkController(
            ToggleShortBookmarkUseCase toggleShortBookmarkUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.toggleShortBookmarkUseCase = toggleShortBookmarkUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/{shortId}/bookmark")
    public ResponseEntity<ApiResponse<ShortBookmarkResponse>> toggleBookmark(
            @PathVariable Long shortId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        ShortBookmarkResult result = toggleShortBookmarkUseCase.toggle(userId, shortId);
        return ResponseEntity.ok(ApiResponse.success(
                "북마크 상태가 변경되었습니다.", ShortBookmarkResponse.from(result)));
    }
}
