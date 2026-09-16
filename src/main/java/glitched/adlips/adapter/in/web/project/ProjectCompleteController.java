package glitched.adlips.adapter.in.web.project;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.response.ProjectCompleteResponse;
import glitched.adlips.application.project.usecase.ProjectCompleteUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "프로젝트 완성", description = "프로젝트 완성 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectCompleteController {
    private final ProjectCompleteUseCase useCase;
    private final AuthenticatedUserResolver resolver;

    public ProjectCompleteController(ProjectCompleteUseCase useCase, AuthenticatedUserResolver resolver) {
        this.useCase = useCase;
        this.resolver = resolver;
    }

    @PostMapping("/{projectId}/complete")
    public ResponseEntity<ApiResponse<ProjectCompleteResponse>> complete(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String authorization
    ) {
        var result = useCase.execute(projectId, resolver.requireUserId(authorization));
        return ResponseEntity.ok(ApiResponse.success("곡이 완성되었습니다.", result));
    }
}
