package glitched.adlips.adapter.in.web.project;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.request.ProjectCreateRequest;
import glitched.adlips.application.project.dto.response.ProjectCreateResponse;
import glitched.adlips.application.project.usecase.ProjectCreateUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "프로젝트", description = "프로젝트 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
    private final ProjectCreateUseCase projectCreateUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ProjectController(ProjectCreateUseCase projectCreateUseCase,
                             AuthenticatedUserResolver authenticatedUserResolver) {
        this.projectCreateUseCase = projectCreateUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectCreateResponse>> create(
            @RequestHeader("Authorization") String authorization,
            @RequestBody ProjectCreateRequest request) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        ProjectCreateResponse response = projectCreateUseCase.execute(new ProjectCreateRequest(
                userId, request.title(), request.description(), request.albumImageFileId()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("곡이 생성되었습니다.", response));
    }
}
