package glitched.adlips.adapter.in.web.shorts;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.shorts.GetRecommendedShortsUseCase;
import glitched.adlips.application.shorts.ShortRecommendationPage;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "숏폼", description = "숏폼 조회 API")
@RequestMapping("/api/v1/shorts/recommended")
public class ShortRecommendationController {
    private final GetRecommendedShortsUseCase recommendations;
    private final AuthenticatedUserResolver users;

    public ShortRecommendationController(GetRecommendedShortsUseCase recommendations, AuthenticatedUserResolver users) {
        this.recommendations = recommendations;
        this.users = users;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ShortRecommendationPage>> getRecommended(
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String cursor,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Long userId = users.resolveOptionalUserId(authorization);
        try {
            ShortRecommendationPage page = recommendations.execute(userId, Math.clamp(size, 1, 100), cursor);
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .body(ApiResponse.success("추천 음악 조회가 완료되었습니다.", page));
        } catch (GetRecommendedShortsUseCase.CursorException exception) {
            HttpStatus status = exception.expired() ? HttpStatus.GONE : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).cacheControl(CacheControl.noStore())
                    .body(new ApiResponse<>(false, exception.getMessage(), null));
        }
    }
}
