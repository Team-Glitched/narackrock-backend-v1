package glitched.adlips.adapter.in.web.shorts;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.ShortsMuteResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.user.profile.usecase.ProfileMuteToggleUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shorts")
public class ShortsMuteSettingController {

    private final ProfileMuteToggleUseCase profileMuteToggleUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ShortsMuteSettingController(
            ProfileMuteToggleUseCase profileMuteToggleUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.profileMuteToggleUseCase = profileMuteToggleUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PatchMapping("/me/settings/mute")
    public ResponseEntity<ApiResponse<ShortsMuteResponse>> toggleMute(
            @RequestHeader("Authorization") String authorization
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        boolean isMuted = profileMuteToggleUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.success(
                "음소거 설정이 변경되었습니다.", new ShortsMuteResponse(isMuted)));
    }
}
