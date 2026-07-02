package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.adapter.out.persistence.project.ProjectContributionJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectMemberJpaRepository;
import glitched.adlips.application.project.dto.request.ProjectContributionGetListRequest;
import glitched.adlips.application.project.usecase.ProjectContributionGetListUseCase;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.domain.project.ContributionApprovalStatus;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectContribution;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class ProjectContributionGetListUseCaseTest {
    @Test
    void ownerGetsPendingAndApprovedContributionsByDefault() {
        User owner = User.create("owner@example.com").withId(1L);
        User contributor = User.create("contributor@example.com").withId(2L);
        Project project = new Project(owner, "곡", null, 701L);
        ProjectContribution contribution = mock(ProjectContribution.class);
        when(contribution.getId()).thenReturn(10L);
        when(contribution.getProject()).thenReturn(project);
        when(contribution.getUser()).thenReturn(contributor);
        when(contribution.getDescription()).thenReturn("리프 추가");
        when(contribution.getBaseMajorVersion()).thenReturn(1);
        when(contribution.getBaseMinorVersion()).thenReturn(1);
        when(contribution.getApprovalStatus()).thenReturn(ContributionApprovalStatus.PENDING);
        when(contribution.getItems()).thenReturn(List.of());
        when(contribution.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 7, 1, 18, 10));

        ProjectJpaRepository projects = mock(ProjectJpaRepository.class);
        ProjectMemberJpaRepository members = mock(ProjectMemberJpaRepository.class);
        ProjectContributionJpaRepository contributions = mock(ProjectContributionJpaRepository.class);
        ProfileRepositoryPort profiles = mock(ProfileRepositoryPort.class);
        MediaFileRepositoryPort mediaFiles = mock(MediaFileRepositoryPort.class);
        when(projects.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(new ProjectMember(project, owner, ProjectMemberRole.OWNER)));
        when(contributions.findByProjectIdAndApprovalStatusInOrderByIdDesc(
                any(), any(), any(Pageable.class))).thenReturn(List.of(contribution));
        when(profiles.findByUserId(2L)).thenReturn(Optional.of(Profile.create(2L, "guitar_moon")));

        var response = new ProjectContributionGetListUseCase(
                projects, members, contributions, profiles, mediaFiles)
                .execute(new ProjectContributionGetListRequest(1L, 1L, null, null, 20));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().nickname()).isEqualTo("guitar_moon");
        assertThat(response.page().nextCursor()).isEqualTo(10L);
        verify(contributions).findByProjectIdAndApprovalStatusInOrderByIdDesc(
                any(), argThat(statuses -> !statuses.contains(ContributionApprovalStatus.REJECTED)),
                any(Pageable.class));
    }
}
