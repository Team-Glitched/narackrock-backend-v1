package glitched.adlips.adapter.in.web.project;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.response.ProjectTimingResponse;
import glitched.adlips.application.project.usecase.ProjectTimingUpdateUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name="프로젝트",description="프로젝트 API")
@SecurityRequirement(name="bearerAuth")
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectTimingController {
    private final ProjectTimingUpdateUseCase timing;
    private final AuthenticatedUserResolver resolver;
    public ProjectTimingController(ProjectTimingUpdateUseCase timing,AuthenticatedUserResolver resolver) {
        this.timing=timing;this.resolver=resolver;
    }
    @PatchMapping("/{projectId}/timing")
    public ApiResponse<ProjectTimingResponse> update(@PathVariable Long projectId,
            @RequestHeader("Authorization") String authorization,@RequestBody UpdateRequest request) {
        Long userId=resolver.requireUserId(authorization);
        if(request==null || request.bpm()==null || request.timeSignatureNumerator()==null || request.timeSignatureDenominator()==null) {
            throw new ProjectApplicationException(ProjectErrorCode.VALIDATION_ERROR,"BPM과 박자표를 입력해 주세요.");
        }
        return ApiResponse.success("곡 설정이 저장되었습니다.",timing.execute(projectId,userId,request.bpm(),request.timeSignatureNumerator(),request.timeSignatureDenominator()));
    }
    public record UpdateRequest(Integer bpm,Integer timeSignatureNumerator,Integer timeSignatureDenominator) {}
}
