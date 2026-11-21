package glitched.adlips.adapter.in.web.opensource;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.opensource.OpenSourceSaveResult;
import glitched.adlips.application.opensource.OpenSourceSaveUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/sources", "/api/v1/open-sources"})
@Tag(name = "샘플 보관함", description = "작곡 시 재사용할 샘플 저장 및 해제")
@SecurityRequirement(name = "bearerAuth")
public class OpenSourceSaveController {
    private final OpenSourceSaveUseCase useCase;
    private final AuthenticatedUserResolver users;

    public OpenSourceSaveController(OpenSourceSaveUseCase useCase, AuthenticatedUserResolver users) {
        this.useCase = useCase;
        this.users = users;
    }

    @PutMapping("/{openSourceId}/save")
    public ApiResponse<OpenSourceSaveResult> save(
            @PathVariable Long openSourceId, @RequestHeader("Authorization") String authorization) {
        return ApiResponse.success("샘플을 보관함에 저장했습니다.",
                useCase.save(users.requireUserId(authorization), openSourceId));
    }

    @DeleteMapping("/{openSourceId}/save")
    public ApiResponse<OpenSourceSaveResult> remove(
            @PathVariable Long openSourceId, @RequestHeader("Authorization") String authorization) {
        return ApiResponse.success("샘플을 보관함에서 제거했습니다.",
                useCase.remove(users.requireUserId(authorization), openSourceId));
    }
}
