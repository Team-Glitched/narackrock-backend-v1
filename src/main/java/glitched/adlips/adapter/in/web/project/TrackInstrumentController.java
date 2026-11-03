package glitched.adlips.adapter.in.web.project;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.usecase.TrackInstrumentUpdateUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name="트랙",description="트랙 API")
@SecurityRequirement(name="bearerAuth")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/tracks/{trackId}/instrument")
public class TrackInstrumentController {
    private final TrackInstrumentUpdateUseCase instruments;
    private final AuthenticatedUserResolver resolver;
    public TrackInstrumentController(TrackInstrumentUpdateUseCase instruments,AuthenticatedUserResolver resolver) {
        this.instruments=instruments;this.resolver=resolver;
    }
    @PatchMapping
    public ApiResponse<Result> update(@PathVariable Long projectId,@PathVariable Long trackId,
            @RequestHeader("Authorization")String authorization,@RequestBody Request request) {
        String instrument=instruments.execute(projectId,trackId,resolver.requireUserId(authorization),request.instrument());
        return ApiResponse.success("악기를 선택했습니다.",new Result(instrument));
    }
    public record Request(String instrument) {}
    public record Result(String instrument) {}
}
