package glitched.adlips.adapter.in.web.project;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.request.TrackCreateRequest;
import glitched.adlips.application.project.dto.response.TrackCreateResponse;
import glitched.adlips.application.project.usecase.TrackCreateUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/tracks")
public class TrackController {
    private final TrackCreateUseCase trackCreateUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public TrackController(TrackCreateUseCase trackCreateUseCase,
                           AuthenticatedUserResolver authenticatedUserResolver) {
        this.trackCreateUseCase = trackCreateUseCase;
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
}
