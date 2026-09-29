package glitched.adlips.adapter.in.web.shorts;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.ShortPlaybackRequest;
import glitched.adlips.adapter.in.web.dto.ShortPlaybackResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.shorts.ShortPlaybackResult;
import glitched.adlips.application.shorts.ToggleShortPlaybackUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "숏폼 재생", description = "숏폼 재생 API")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/shorts")
public class ShortsPlaybackController {

    private final ToggleShortPlaybackUseCase toggleShortPlaybackUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ShortsPlaybackController(
            ToggleShortPlaybackUseCase toggleShortPlaybackUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.toggleShortPlaybackUseCase = toggleShortPlaybackUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PatchMapping("/{shortId}/playback/toggle")
    public ResponseEntity<ApiResponse<ShortPlaybackResponse>> togglePlayback(
            @PathVariable Long shortId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody ShortPlaybackRequest request
    ) {
        authenticatedUserResolver.requireUserId(authorization);
        ShortPlaybackResult result = toggleShortPlaybackUseCase.toggle(
                shortId,
                request.isPlaying(),
                request.currentTime()
        );
        return ResponseEntity.ok(ApiResponse.success("성공", ShortPlaybackResponse.from(result)));
    }
}
