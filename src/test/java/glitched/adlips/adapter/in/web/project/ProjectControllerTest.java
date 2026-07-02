package glitched.adlips.adapter.in.web.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.request.ProjectCreateRequest;
import glitched.adlips.application.project.dto.response.ProjectCreateResponse;
import glitched.adlips.application.project.dto.response.ProjectVersionResponse;
import glitched.adlips.application.project.usecase.ProjectCreateUseCase;
import glitched.adlips.domain.project.ProjectStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class ProjectControllerTest {
    @Test
    void returnsCreatedForProjectCreation() {
        ProjectCreateUseCase useCase = mock(ProjectCreateUseCase.class);
        AuthenticatedUserResolver resolver = mock(AuthenticatedUserResolver.class);
        when(resolver.requireUserId("Bearer token")).thenReturn(1L);
        when(useCase.execute(any())).thenReturn(new ProjectCreateResponse(
                10L, "새 곡", null, 701L, "url", 1L,
                new ProjectVersionResponse(1, 1, "v1.1"), ProjectStatus.DRAFT, false, null));

        ResponseEntity<ApiResponse<ProjectCreateResponse>> result = new ProjectController(useCase, resolver)
                .create("Bearer token", new ProjectCreateRequest(null, "새 곡", null, 701L));

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody().message()).isEqualTo("곡이 생성되었습니다.");
    }
}
