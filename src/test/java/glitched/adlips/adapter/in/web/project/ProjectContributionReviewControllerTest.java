package glitched.adlips.adapter.in.web.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.request.ProjectContributionReviewRequest;
import glitched.adlips.application.project.dto.response.ProjectContributionReviewResponse;
import glitched.adlips.application.project.dto.response.ProjectVersionResponse;
import glitched.adlips.application.project.usecase.ProjectContributionReviewUseCase;
import glitched.adlips.domain.project.ContributionApprovalStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ProjectContributionReviewControllerTest {
    @Test
    void returnsApprovalMessage() {
        ProjectContributionReviewUseCase useCase = mock(ProjectContributionReviewUseCase.class);
        AuthenticatedUserResolver resolver = mock(AuthenticatedUserResolver.class);
        when(resolver.requireUserId("Bearer token")).thenReturn(1L);
        when(useCase.execute(any())).thenReturn(new ProjectContributionReviewResponse(
                1L, 10L, ContributionApprovalStatus.APPROVED,
                new ProjectVersionResponse(1, 1, "v1.1"),
                new ProjectVersionResponse(2, 1, "v2.1"),
                1L, LocalDateTime.now(), "좋습니다."));

        ApiResponse<ProjectContributionReviewResponse> result =
                new ProjectContributionReviewController(useCase, resolver).review(
                        1L, 10L, "Bearer token",
                        new ProjectContributionReviewRequest(null, null, null,
                                ContributionApprovalStatus.APPROVED, "좋습니다."));

        assertThat(result.message()).isEqualTo("기여 요청이 승인되었습니다.");
    }
}
