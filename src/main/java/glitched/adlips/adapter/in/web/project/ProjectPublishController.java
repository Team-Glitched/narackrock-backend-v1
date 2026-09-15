package glitched.adlips.adapter.in.web.project;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.request.ProjectPublishRequest;
import glitched.adlips.application.project.dto.response.ProjectPublishResponse;
import glitched.adlips.application.project.usecase.ProjectPublishUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "프로젝트 발행", description = "프로젝트 발행 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectPublishController {
    private final ProjectPublishUseCase useCase;
    private final AuthenticatedUserResolver resolver;
    public ProjectPublishController(ProjectPublishUseCase useCase, AuthenticatedUserResolver resolver) {
        this.useCase = useCase; this.resolver = resolver;
    }
    @PostMapping("/{projectId}/exports")
    public ResponseEntity<ApiResponse<ProjectPublishResponse>> publish(
            @PathVariable Long projectId, @RequestHeader("Authorization") String authorization) {
        var response = useCase.execute(new ProjectPublishRequest(projectId, resolver.requireUserId(authorization)));
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("곡 공개 및 최종 믹싱 작업이 시작되었습니다.", response));
    }
}
