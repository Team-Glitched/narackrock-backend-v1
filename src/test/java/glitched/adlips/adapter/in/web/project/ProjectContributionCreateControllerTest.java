package glitched.adlips.adapter.in.web.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.request.ProjectContributionCreateRequest;
import glitched.adlips.application.project.dto.response.ProjectContributionCreateResponse;
import glitched.adlips.application.project.dto.response.ProjectVersionResponse;
import glitched.adlips.application.project.usecase.ProjectContributionCreateUseCase;
import glitched.adlips.domain.project.ContributionApprovalStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class ProjectContributionCreateControllerTest {
    @Test
    void returnsCreatedForContributionRequest() {
        ProjectContributionCreateUseCase useCase = mock(ProjectContributionCreateUseCase.class);
        AuthenticatedUserResolver resolver = mock(AuthenticatedUserResolver.class);
        when(resolver.requireUserId("Bearer token")).thenReturn(2L);
        when(useCase.execute(any())).thenReturn(new ProjectContributionCreateResponse(
                1L, 10L, new ProjectVersionResponse(1, 1, "v1.1"),
                ContributionApprovalStatus.PENDING, "리프 추가", List.of(), LocalDateTime.now()));

        ResponseEntity<ApiResponse<ProjectContributionCreateResponse>> result =
                new ProjectContributionCreateController(useCase, resolver).create(
                        1L, "Bearer token",
                        new ProjectContributionCreateRequest(null, null, "리프 추가", null, List.of()));

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody().message()).isEqualTo("곡 기여 요청이 전송되었습니다.");
    }
}
