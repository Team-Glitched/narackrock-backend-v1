package glitched.adlips.adapter.in.web.project;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.request.TrackDeleteRequest;
import glitched.adlips.application.project.dto.response.TrackDeleteResponse;
import glitched.adlips.application.project.usecase.TrackDeleteUseCase;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tracks")
public class TrackDeleteController {
    private final TrackDeleteUseCase useCase;
    private final AuthenticatedUserResolver resolver;
    public TrackDeleteController(TrackDeleteUseCase useCase, AuthenticatedUserResolver resolver) {
        this.useCase = useCase; this.resolver = resolver;
    }
    @DeleteMapping("/{trackId}")
    public ApiResponse<TrackDeleteResponse> delete(@PathVariable Long trackId,
                                                   @RequestHeader("Authorization") String authorization) {
        return ApiResponse.success("트랙이 삭제되었습니다.",
                useCase.execute(new TrackDeleteRequest(trackId, resolver.requireUserId(authorization))));
    }
}
