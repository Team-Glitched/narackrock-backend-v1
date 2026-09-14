package glitched.adlips.adapter.in.web.project;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.request.ProjectExportGetRequest;
import glitched.adlips.application.project.dto.response.ProjectExportGetResponse;
import glitched.adlips.application.project.usecase.ProjectExportGetUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectExportController {

    private final ProjectExportGetUseCase useCase;
    private final AuthenticatedUserResolver resolver;

    public ProjectExportController(ProjectExportGetUseCase useCase, AuthenticatedUserResolver resolver) {
        this.useCase = useCase;
        this.resolver = resolver;
    }

    @GetMapping("/{projectId}/exports/{exportId}")
    public ResponseEntity<ApiResponse<ProjectExportGetResponse>> getStatus(
            @PathVariable Long projectId,
            @PathVariable Long exportId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long userId = resolver.requireUserId(authorization);
        var response = useCase.execute(new ProjectExportGetRequest(projectId, exportId, userId));
        return ResponseEntity.ok(ApiResponse.success("Export 상태를 조회했습니다.", response));
    }
}
