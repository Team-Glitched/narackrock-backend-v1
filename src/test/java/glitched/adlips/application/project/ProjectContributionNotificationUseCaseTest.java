package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.application.project.port.out.ProjectContributionRepositoryPort;
import glitched.adlips.application.project.usecase.ProjectContributionNotificationUseCase;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.domain.project.ContributionApprovalStatus;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectContribution;
import glitched.adlips.domain.project.ProjectContributionItem;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProjectContributionNotificationUseCaseTest {

    @Test
    void returnsPendingContributionsForProjectsOwnedByTheCurrentUser() {
        ProjectContributionRepositoryPort contributions = mock(ProjectContributionRepositoryPort.class);
        ProfileRepositoryPort profiles = mock(ProfileRepositoryPort.class);
        ProjectContribution contribution = mock(ProjectContribution.class);
        Project project = mock(Project.class);
        User contributor = mock(User.class);
        Profile profile = mock(Profile.class);
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 29, 22, 0);

        when(contributions.findByProjectOwnerIdAndApprovalStatusOrderByIdDesc(
                1L, ContributionApprovalStatus.PENDING)).thenReturn(List.of(contribution));
        when(contribution.getId()).thenReturn(31L);
        when(contribution.getProject()).thenReturn(project);
        when(project.getId()).thenReturn(8L);
        when(project.getTitle()).thenReturn("함께 만드는 곡");
        when(contribution.getUser()).thenReturn(contributor);
        when(contributor.getId()).thenReturn(2L);
        when(profiles.findByUserId(2L)).thenReturn(Optional.of(profile));
        when(profile.getNickname()).thenReturn("기타소년");
        when(contribution.getDescription()).thenReturn("기타 레이어를 추가했어요");
        when(contribution.getItems()).thenReturn(List.of(mock(ProjectContributionItem.class)));
        when(contribution.getCreatedAt()).thenReturn(createdAt);

        var result = new ProjectContributionNotificationUseCase(contributions, profiles).execute(1L);

        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.contributionId()).isEqualTo(31L);
            assertThat(item.projectId()).isEqualTo(8L);
            assertThat(item.projectTitle()).isEqualTo("함께 만드는 곡");
            assertThat(item.nickname()).isEqualTo("기타소년");
            assertThat(item.itemCount()).isEqualTo(1);
            assertThat(item.createdAt()).isEqualTo(createdAt);
        });
    }
}
