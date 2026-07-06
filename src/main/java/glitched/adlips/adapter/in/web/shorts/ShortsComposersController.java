package glitched.adlips.adapter.in.web.shorts;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.ShortsComposerResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.shorts.GetShortsComposersUseCase;
import glitched.adlips.application.shorts.port.out.ShortsComposerQueryItem;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shorts")
public class ShortsComposersController {

    private final GetShortsComposersUseCase getShortsComposersUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ShortsComposersController(
            GetShortsComposersUseCase getShortsComposersUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.getShortsComposersUseCase = getShortsComposersUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping("/{shortId}/composers")
    public ResponseEntity<ApiResponse<List<ShortsComposerResponse>>> getComposers(
            @PathVariable Long shortId,
            @RequestHeader("Authorization") String authorization
    ) {
        authenticatedUserResolver.requireUserId(authorization);
        List<ShortsComposerQueryItem> items = getShortsComposersUseCase.execute(shortId);
        return ResponseEntity.ok(ApiResponse.success(
                "참여자 목록 조회가 완료되었습니다.",
                items.stream().map(ShortsComposerResponse::from).toList()));
    }
}
