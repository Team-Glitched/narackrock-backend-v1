package glitched.adlips.adapter.in.web.project;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.request.ProjectContributionCreateRequest;
import glitched.adlips.application.project.dto.response.ProjectContributionCreateResponse;
import glitched.adlips.application.project.usecase.ProjectContributionCreateUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/contributions")
public class ProjectContributionCreateController {
    private final ProjectContributionCreateUseCase projectContributionCreateUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ProjectContributionCreateController(
            ProjectContributionCreateUseCase projectContributionCreateUseCase,
            AuthenticatedUserResolver authenticatedUserResolver) {
        this.projectContributionCreateUseCase = projectContributionCreateUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectContributionCreateResponse>> create(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody ProjectContributionCreateRequest request) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        var response = projectContributionCreateUseCase.execute(new ProjectContributionCreateRequest(
                projectId, userId, request.description(), request.baseProjectVersion(), request.items()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("곡 기여 요청이 전송되었습니다.", response));
    }
}
