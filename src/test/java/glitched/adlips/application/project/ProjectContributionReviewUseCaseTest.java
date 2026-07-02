package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.adapter.out.persistence.project.ProjectClipJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectContributionJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectMemberJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectTrackJpaRepository;
import glitched.adlips.application.project.dto.request.ProjectContributionReviewRequest;
import glitched.adlips.application.project.usecase.ProjectContributionReviewUseCase;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ContributionApprovalStatus;
import glitched.adlips.domain.project.ContributionChangeType;
import glitched.adlips.domain.project.ContributionTargetType;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectContribution;
import glitched.adlips.domain.project.ProjectContributionItem;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.project.ProjectTrack;
import glitched.adlips.domain.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProjectContributionReviewUseCaseTest {
    @Test
    void approvesAddContributionAndIncreasesMajorVersion() {
        Fixture fixture = new Fixture();

        var response = fixture.useCase.execute(new ProjectContributionReviewRequest(
                1L, 10L, 1L, ContributionApprovalStatus.APPROVED, "좋습니다."));

        assertThat(response.approvalStatus()).isEqualTo(ContributionApprovalStatus.APPROVED);
        assertThat(response.previousProjectVersion().display()).isEqualTo("v1.1");
        assertThat(response.projectVersion().display()).isEqualTo("v2.1");
        assertThat(fixture.track.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
    }

    @Test
    void rejectsContributionWithoutChangingProjectVersion() {
        Fixture fixture = new Fixture();

        var response = fixture.useCase.execute(new ProjectContributionReviewRequest(
                1L, 10L, 1L, ContributionApprovalStatus.REJECTED, "방향이 맞지 않습니다."));

        assertThat(response.projectVersion().display()).isEqualTo("v1.1");
        assertThat(fixture.track.getApprovalStatus()).isEqualTo(ApprovalStatus.REJECTED);
    }

    private static class Fixture {
        private final User reviewer = User.create("owner@example.com").withId(1L);
        private final User contributor = User.create("contributor@example.com").withId(2L);
        private final Project project = new Project(reviewer, "곡", null, 701L);
        private final ProjectTrack track = new ProjectTrack(project, contributor, "Guitar", "GUITAR", 0);
        private final ProjectContribution contribution = new ProjectContribution(project, contributor, "리프 추가");
        private final ProjectContributionReviewUseCase useCase;

        private Fixture() {
            track.submitForReview();
            new ProjectContributionItem(contribution, ContributionChangeType.ADD,
                    ContributionTargetType.TRACK, 101L);
            ProjectContributionJpaRepository contributions = mock(ProjectContributionJpaRepository.class);
            ProjectMemberJpaRepository members = mock(ProjectMemberJpaRepository.class);
            ProjectTrackJpaRepository tracks = mock(ProjectTrackJpaRepository.class);
            ProjectClipJpaRepository clips = mock(ProjectClipJpaRepository.class);
            UserRepositoryPort users = mock(UserRepositoryPort.class);
            when(contributions.findByIdAndProjectId(10L, 1L)).thenReturn(Optional.of(contribution));
            when(members.findByProjectIdAndUserId(1L, 1L))
                    .thenReturn(Optional.of(new ProjectMember(project, reviewer, ProjectMemberRole.OWNER)));
            when(users.findById(1L)).thenReturn(Optional.of(reviewer));
            when(tracks.findByIdAndIsDeletedFalse(101L)).thenReturn(Optional.of(track));
            useCase = new ProjectContributionReviewUseCase(contributions, members, tracks, clips, users);
        }
    }
}
