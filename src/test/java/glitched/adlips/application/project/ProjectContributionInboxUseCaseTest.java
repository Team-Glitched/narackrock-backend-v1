package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.application.project.port.out.ProjectContributionRepositoryPort;
import glitched.adlips.application.project.usecase.ProjectContributionInboxUseCase;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectContribution;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ProjectContributionInboxUseCaseTest {
    @Test
    void returnsPendingRequestsForTheOwnerWithSongAndComment() {
        var owner = User.create("owner@example.com").withId(1L);
        var contributor = User.create("contributor@example.com").withId(2L);
        var project = new Project(owner, "테스트 곡", null, 0L);
        ReflectionTestUtils.setField(project, "id", 17L);
        var contribution = new ProjectContribution(project, contributor, "기타 연주를 더했습니다.");
        ReflectionTestUtils.setField(contribution, "id", 31L);
        var repository = mock(ProjectContributionRepositoryPort.class);
        var profiles = mock(ProfileRepositoryPort.class);
        when(repository.findPendingForOwner(1L, 50)).thenReturn(List.of(contribution));
        when(profiles.findByUserId(2L)).thenReturn(Optional.of(Profile.create(2L, "연주자")));

        var result = new ProjectContributionInboxUseCase(repository, profiles).execute(1L);

        assertThat(result.items()).hasSize(1);
        var item = result.items().getFirst();
        assertThat(item.contributionId()).isEqualTo(31L);
        assertThat(item.projectId()).isEqualTo(17L);
        assertThat(item.projectTitle()).isEqualTo("테스트 곡");
        assertThat(item.nickname()).isEqualTo("연주자");
        assertThat(item.description()).isEqualTo("기타 연주를 더했습니다.");
    }
}
