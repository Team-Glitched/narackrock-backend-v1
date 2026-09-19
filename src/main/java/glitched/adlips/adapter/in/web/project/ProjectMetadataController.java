package glitched.adlips.adapter.in.web.project;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.usecase.ProjectMetadataUpdateUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "프로젝트", description = "프로젝트 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectMetadataController {
    private final ProjectMetadataUpdateUseCase metadata;
    private final AuthenticatedUserResolver resolver;

    public ProjectMetadataController(ProjectMetadataUpdateUseCase metadata, AuthenticatedUserResolver resolver) {
        this.metadata = metadata;
        this.resolver = resolver;
    }

    @PatchMapping("/{projectId}/metadata")
    public ApiResponse<Void> update(@PathVariable Long projectId, @RequestHeader("Authorization") String authorization,
                                    @RequestBody UpdateRequest request) {
        metadata.execute(projectId, resolver.requireUserId(authorization), request.title(), request.albumImageFileId());
        return ApiResponse.success("곡 정보가 저장되었습니다.");
    }

    public record UpdateRequest(String title, Long albumImageFileId) {}
}
