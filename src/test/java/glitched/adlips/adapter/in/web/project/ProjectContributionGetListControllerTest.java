package glitched.adlips.adapter.in.web.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.response.ProjectContributionGetListResponse;
import glitched.adlips.application.project.usecase.ProjectContributionGetListUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectContributionGetListControllerTest {
    @Test
    void returnsContributionList() {
        ProjectContributionGetListUseCase useCase = mock(ProjectContributionGetListUseCase.class);
        AuthenticatedUserResolver resolver = mock(AuthenticatedUserResolver.class);
        when(resolver.requireUserId("Bearer token")).thenReturn(1L);
        when(useCase.execute(any())).thenReturn(new ProjectContributionGetListResponse(
                List.of(), new ProjectContributionGetListResponse.Page(null, false, 20)));

        ApiResponse<ProjectContributionGetListResponse> result =
                new ProjectContributionGetListController(useCase, resolver)
                        .getList(1L, "Bearer token", null, null, 20);

        assertThat(result.message()).isEqualTo("기여 요청 목록 조회가 완료되었습니다.");
        assertThat(result.data().page().size()).isEqualTo(20);
    }
}
