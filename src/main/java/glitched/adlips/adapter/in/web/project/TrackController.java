package glitched.adlips.adapter.in.web.project;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.request.OpenSourceTrackCreateRequest;
import glitched.adlips.application.project.dto.request.TrackCreateRequest;
import glitched.adlips.application.project.dto.response.AudioClipCreateResponse;
import glitched.adlips.application.project.dto.response.TrackCreateResponse;
import glitched.adlips.application.project.usecase.OpenSourceClipCreateUseCase;
import glitched.adlips.application.project.usecase.TrackCreateUseCase;
import glitched.adlips.application.project.dto.request.TrackGetListRequest;
import glitched.adlips.application.project.dto.response.TrackGetListResponse;
import glitched.adlips.application.project.usecase.TrackGetListUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "트랙", description = "트랙 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/tracks")
public class TrackController {
    private final TrackCreateUseCase trackCreateUseCase;
    private final TrackGetListUseCase trackGetListUseCase;
    private final OpenSourceClipCreateUseCase openSourceClipCreateUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public TrackController(TrackCreateUseCase trackCreateUseCase, TrackGetListUseCase trackGetListUseCase,
                           OpenSourceClipCreateUseCase openSourceClipCreateUseCase,
                           AuthenticatedUserResolver authenticatedUserResolver) {
        this.trackCreateUseCase = trackCreateUseCase;
        this.trackGetListUseCase = trackGetListUseCase;
        this.openSourceClipCreateUseCase = openSourceClipCreateUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TrackCreateResponse>> create(
            @PathVariable Long projectId, @RequestHeader("Authorization") String authorization,
            @RequestBody TrackCreateRequest request) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        TrackCreateResponse response = trackCreateUseCase.execute(new TrackCreateRequest(
                projectId, userId, request.name(), request.instrument(), request.sortOrder()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("트랙이 생성되었습니다.", response));
    }

    @PostMapping({"/source", "/open-source"})
    public ResponseEntity<ApiResponse<AudioClipCreateResponse>> createFromOpenSource(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody OpenSourceTrackCreateRequest request) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        var response = openSourceClipCreateUseCase.createTrack(new OpenSourceTrackCreateRequest(
                projectId, userId, request.openSourceId(), request.startTick(), request.durationTick()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("샘플을 새 트랙으로 추가했습니다.", response));
    }

    @GetMapping
    public ApiResponse<TrackGetListResponse> getList(
            @PathVariable Long projectId, @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "STUDIO") TrackGetListRequest.View view,
            @RequestParam(required = false) Long contributionId) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        return ApiResponse.success("트랙 목록 조회가 완료되었습니다.",
                trackGetListUseCase.execute(new TrackGetListRequest(projectId, userId, view, contributionId)));
    }
}
