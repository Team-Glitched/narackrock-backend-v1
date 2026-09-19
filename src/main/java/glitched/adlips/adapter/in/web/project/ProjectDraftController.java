package glitched.adlips.adapter.in.web.project;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.response.ProjectCreateResponse;
import glitched.adlips.application.project.usecase.ProjectDraftCreateUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "프로젝트", description = "프로젝트 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectDraftController {
    private final ProjectDraftCreateUseCase drafts;
    private final AuthenticatedUserResolver resolver;

    public ProjectDraftController(ProjectDraftCreateUseCase drafts, AuthenticatedUserResolver resolver) {
        this.drafts = drafts;
        this.resolver = resolver;
    }

    @PostMapping("/drafts")
    public ResponseEntity<ApiResponse<ProjectCreateResponse>> create(@RequestHeader("Authorization") String authorization) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("작곡 초안이 생성되었습니다.", drafts.execute(resolver.requireUserId(authorization))));
    }
}
