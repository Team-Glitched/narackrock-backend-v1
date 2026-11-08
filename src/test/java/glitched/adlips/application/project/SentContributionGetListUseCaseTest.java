package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import glitched.adlips.application.project.dto.request.SentContributionGetListRequest;
import glitched.adlips.application.project.port.out.ProjectContributionRepositoryPort;
import glitched.adlips.application.project.usecase.SentContributionGetListUseCase;
import glitched.adlips.domain.project.*;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class SentContributionGetListUseCaseTest {
    private final ProjectContributionRepositoryPort repository = mock(ProjectContributionRepositoryPort.class);

    @Test
    void listsRequestsAcrossProjectsWithRejectedHistoryAndCursor() {
        var first = contribution(30L, 41L, "첫 곡", ContributionApprovalStatus.REJECTED);
        var second = contribution(29L, 42L, "다른 곡", ContributionApprovalStatus.APPROVED);
        when(repository.findSentByUserId(7L, null, 2)).thenReturn(List.of(first, second));
        var result = new SentContributionGetListUseCase(repository)
                .execute(new SentContributionGetListRequest(7L, null, 1));
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().projectTitle()).isEqualTo("첫 곡");
        assertThat(result.items().getFirst().approvalStatus()).isEqualTo(ContributionApprovalStatus.REJECTED);
        assertThat(result.page().hasMore()).isTrue();
        assertThat(result.page().nextCursor()).isEqualTo(30L);
        when(repository.findSentByUserId(7L, 30L, 2)).thenReturn(List.of(second));
        var next = new SentContributionGetListUseCase(repository)
                .execute(new SentContributionGetListRequest(7L, 30L, 1));
        assertThat(next.items().getFirst().projectTitle()).isEqualTo("다른 곡");
        assertThat(next.page().hasMore()).isFalse();
        verify(repository).findSentByUserId(7L, 30L, 2);
    }

    @Test
    void rejectsInvalidPagingAndUserBeforeReadingData() {
        var useCase = new SentContributionGetListUseCase(repository);
        for (var request : List.of(new SentContributionGetListRequest(7L, null, 0),
                new SentContributionGetListRequest(7L, null, 101),
                new SentContributionGetListRequest(7L, 0L, 20),
                new SentContributionGetListRequest(0L, null, 20))) {
            assertThatThrownBy(() -> useCase.execute(request)).isInstanceOf(ProjectApplicationException.class);
        }
        verifyNoInteractions(repository);
    }

    private ProjectContribution contribution(long id, long projectId, String title, ContributionApprovalStatus status) {
        var request = mock(ProjectContribution.class);
        var project = mock(Project.class);
        when(project.getId()).thenReturn(projectId);
        when(project.getTitle()).thenReturn(title);
        when(request.getId()).thenReturn(id);
        when(request.getProject()).thenReturn(project);
        when(request.getApprovalStatus()).thenReturn(status);
        when(request.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 11, 8, 12, 0));
        return request;
    }
}
