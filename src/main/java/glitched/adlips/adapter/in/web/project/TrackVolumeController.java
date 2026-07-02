package glitched.adlips.adapter.in.web.project;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.request.TrackVolumeUpdateRequest;
import glitched.adlips.application.project.dto.response.TrackVolumeUpdateResponse;
import glitched.adlips.application.project.usecase.TrackVolumeUpdateUseCase;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tracks/{trackId}/volume")
public class TrackVolumeController {
    private final TrackVolumeUpdateUseCase trackVolumeUpdateUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public TrackVolumeController(TrackVolumeUpdateUseCase trackVolumeUpdateUseCase,
                                 AuthenticatedUserResolver authenticatedUserResolver) {
        this.trackVolumeUpdateUseCase = trackVolumeUpdateUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PatchMapping
    public ApiResponse<TrackVolumeUpdateResponse> update(
            @PathVariable Long trackId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody TrackVolumeUpdateRequest request) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        var response = trackVolumeUpdateUseCase.execute(
                new TrackVolumeUpdateRequest(trackId, userId, request.volume()));
        return ApiResponse.success("트랙 볼륨이 조절되었습니다.", response);
    }
}
