package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.adapter.out.persistence.project.ProjectClipJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectContributionItemJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectContributionJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectMemberJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectTrackJpaRepository;
import glitched.adlips.application.project.dto.request.ProjectContributionCreateRequest;
import glitched.adlips.application.project.usecase.ProjectContributionCreateUseCase;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ContributionChangeType;
import glitched.adlips.domain.project.ContributionTargetType;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectContribution;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.project.ProjectTrack;
import glitched.adlips.domain.user.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProjectContributionCreateUseCaseTest {
    private final ProjectJpaRepository projects = mock(ProjectJpaRepository.class);
    private final ProjectMemberJpaRepository members = mock(ProjectMemberJpaRepository.class);
    private final ProjectContributionJpaRepository contributions = mock(ProjectContributionJpaRepository.class);
    private final ProjectContributionItemJpaRepository contributionItems = mock(ProjectContributionItemJpaRepository.class);
    private final ProjectTrackJpaRepository tracks = mock(ProjectTrackJpaRepository.class);
    private final ProjectClipJpaRepository clips = mock(ProjectClipJpaRepository.class);
    private final UserRepositoryPort users = mock(UserRepositoryPort.class);
    private final User contributor = User.create("contributor@example.com").withId(2L);
    private final User owner = User.create("owner@example.com").withId(1L);
    private final Project project = new Project(owner, "곡", null, 701L);
    private final ProjectTrack track = new ProjectTrack(project, contributor, "Guitar", "GUITAR", 0);

    @BeforeEach
    void setUp() {
        when(projects.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(1L, 2L))
                .thenReturn(Optional.of(new ProjectMember(project, contributor, ProjectMemberRole.EDITOR)));
        when(users.findById(2L)).thenReturn(Optional.of(contributor));
        when(tracks.findByIdAndIsDeletedFalse(101L)).thenReturn(Optional.of(track));
        when(clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(101L)).thenReturn(List.of());
        when(contributions.save(any(ProjectContribution.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsAddContributionAndMarksTargetPending() {
        var response = useCase().execute(new ProjectContributionCreateRequest(
                1L, 2L, "메인 기타 리프", null,
                List.of(new ProjectContributionCreateRequest.Item(
                        ContributionChangeType.ADD, ContributionTargetType.TRACK, 101L))));

        assertThat(response.projectId()).isEqualTo(1L);
        assertThat(response.approvalStatus().name()).isEqualTo("PENDING");
        assertThat(response.baseProjectVersion().display()).isEqualTo("v1.1");
        assertThat(response.items()).hasSize(1);
        assertThat(track.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
    }

    @Test
    void rejectsUpdateWithoutBaseProjectVersion() {
        track.approve();
        var request = new ProjectContributionCreateRequest(
                1L, 2L, "수정", null,
                List.of(new ProjectContributionCreateRequest.Item(
                        ContributionChangeType.UPDATE, ContributionTargetType.TRACK, 101L)));

        assertThatThrownBy(() -> useCase().execute(request))
                .isInstanceOf(ProjectApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ProjectErrorCode.INVALID_CONTRIBUTION_ITEM);
    }

    private ProjectContributionCreateUseCase useCase() {
        return new ProjectContributionCreateUseCase(
                projects, members, contributions, contributionItems, tracks, clips, users);
    }
}
