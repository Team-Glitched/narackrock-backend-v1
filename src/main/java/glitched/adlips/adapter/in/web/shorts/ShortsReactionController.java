package glitched.adlips.adapter.in.web.shorts;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.ShortDislikeResponse;
import glitched.adlips.adapter.in.web.dto.ShortLikeResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.shorts.ShortDislikeResult;
import glitched.adlips.application.shorts.ShortLikeResult;
import glitched.adlips.application.shorts.ToggleShortDislikeUseCase;
import glitched.adlips.application.shorts.ToggleShortLikeUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shorts")
public class ShortsReactionController {

    private final ToggleShortLikeUseCase toggleShortLikeUseCase;
    private final ToggleShortDislikeUseCase toggleShortDislikeUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ShortsReactionController(
            ToggleShortLikeUseCase toggleShortLikeUseCase,
            ToggleShortDislikeUseCase toggleShortDislikeUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.toggleShortLikeUseCase = toggleShortLikeUseCase;
        this.toggleShortDislikeUseCase = toggleShortDislikeUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/{shortId}/like")
    public ResponseEntity<ApiResponse<ShortLikeResponse>> toggleLike(
            @PathVariable Long shortId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        ShortLikeResult result = toggleShortLikeUseCase.toggle(userId, shortId);
        return ResponseEntity.ok(ApiResponse.success(
                "숏폼 좋아요 상태가 변경되었습니다.", ShortLikeResponse.from(result)));
    }

    @PostMapping("/{shortId}/dislike")
    public ResponseEntity<ApiResponse<ShortDislikeResponse>> toggleDislike(
            @PathVariable Long shortId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        ShortDislikeResult result = toggleShortDislikeUseCase.toggle(userId, shortId);
        return ResponseEntity.ok(ApiResponse.success(
                "숏폼 싫어요 상태가 변경되었습니다.", ShortDislikeResponse.from(result)));
    }
}
