package glitched.adlips.adapter.in.web.project;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.request.ProjectContributionGetListRequest;
import glitched.adlips.application.project.dto.response.ProjectContributionGetListResponse;
import glitched.adlips.application.project.usecase.ProjectContributionGetListUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/contributions")
public class ProjectContributionGetListController {
    private final ProjectContributionGetListUseCase projectContributionGetListUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ProjectContributionGetListController(
            ProjectContributionGetListUseCase projectContributionGetListUseCase,
            AuthenticatedUserResolver authenticatedUserResolver) {
        this.projectContributionGetListUseCase = projectContributionGetListUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping
    public ApiResponse<ProjectContributionGetListResponse> getList(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        var response = projectContributionGetListUseCase.execute(
                new ProjectContributionGetListRequest(projectId, userId, status, cursor, size));
        return ApiResponse.success("기여 요청 목록 조회가 완료되었습니다.", response);
    }
}
