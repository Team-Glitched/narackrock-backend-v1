package glitched.adlips.adapter.in.web.project;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.request.ProjectDeleteRequest;
import glitched.adlips.application.project.dto.response.ProjectDeleteResponse;
import glitched.adlips.application.project.usecase.ProjectDeleteUseCase;
import org.springframework.web.bind.annotation.*;

@Tag(name = "프로젝트", description = "프로젝트 삭제 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectDeleteController {
    private final ProjectDeleteUseCase useCase;
    private final AuthenticatedUserResolver resolver;
    public ProjectDeleteController(ProjectDeleteUseCase useCase, AuthenticatedUserResolver resolver) {
        this.useCase = useCase; this.resolver = resolver;
    }
    @DeleteMapping("/{projectId}")
    public ApiResponse<ProjectDeleteResponse> delete(@PathVariable Long projectId,
                                                      @RequestHeader("Authorization") String authorization) {
        return ApiResponse.success("곡이 삭제되었습니다.",
                useCase.execute(new ProjectDeleteRequest(projectId, resolver.requireUserId(authorization))));
    }
}
