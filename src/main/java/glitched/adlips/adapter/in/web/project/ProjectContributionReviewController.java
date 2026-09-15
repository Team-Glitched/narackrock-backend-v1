package glitched.adlips.adapter.in.web.project;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.request.ProjectContributionReviewRequest;
import glitched.adlips.application.project.dto.response.ProjectContributionReviewResponse;
import glitched.adlips.application.project.usecase.ProjectContributionReviewUseCase;
import glitched.adlips.domain.project.ContributionApprovalStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "프로젝트 기여 검토", description = "프로젝트 기여 검토 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/contributions/{contributionId}")
public class ProjectContributionReviewController {
    private final ProjectContributionReviewUseCase projectContributionReviewUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ProjectContributionReviewController(
            ProjectContributionReviewUseCase projectContributionReviewUseCase,
            AuthenticatedUserResolver authenticatedUserResolver) {
        this.projectContributionReviewUseCase = projectContributionReviewUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PatchMapping
    public ApiResponse<ProjectContributionReviewResponse> review(
            @PathVariable Long projectId,
            @PathVariable Long contributionId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody ProjectContributionReviewRequest request) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        var response = projectContributionReviewUseCase.execute(new ProjectContributionReviewRequest(
                projectId, contributionId, userId, request.approvalStatus(), request.reviewComment()));
        String message = response.approvalStatus() == ContributionApprovalStatus.APPROVED
                ? "기여 요청이 승인되었습니다."
                : "기여 요청이 거부되었습니다.";
        return ApiResponse.success(message, response);
    }
}
